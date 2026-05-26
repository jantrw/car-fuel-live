package io.github.jantrw.carfuellive.tankerkoenig.service;

import io.github.jantrw.carfuellive.common.exception.FuelPriceLookupException;
import io.github.jantrw.carfuellive.stations.model.GasStation;
import io.github.jantrw.carfuellive.tankerkoenig.config.TankerkoenigProperties;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Tankerkönig REST client for the live fuel-price MVP.
 *
 * <p>The client issues {@code list.php} requests, validates the upstream payload, and normalizes
 * the usable station data into {@link GasStation} records for the backend.
 */
@Component
public class TankerkoenigRestClient implements TankerkoenigStationSearchClient {

  private static final int DEFAULT_RADIUS_KM = 5;
  private static final String DEFAULT_TYPE = "all";
  private static final String DEFAULT_SORT = "dist";
  private static final JsonParser JSON_PARSER = JsonParserFactory.getJsonParser();

  private final RestClient tankerkoenigRestClient;
  private final TankerkoenigProperties tankerkoenigProperties;

  public TankerkoenigRestClient(
      @Qualifier("tankerkoenigApiRestClient") RestClient tankerkoenigRestClient,
      TankerkoenigProperties tankerkoenigProperties) {
    this.tankerkoenigRestClient = tankerkoenigRestClient;
    this.tankerkoenigProperties = tankerkoenigProperties;
  }

  @Override
  public List<GasStation> searchStations(double latitude, double longitude) {
    final String apiKey = tankerkoenigProperties.apiKey().trim();
    if (apiKey.isEmpty()) {
      throw new FuelPriceLookupException("Tankerkönig API key is not configured.");
    }

    final String responseBody;
    try {
      responseBody =
          tankerkoenigRestClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path("/json/list.php")
                          .queryParam("lat", latitude)
                          .queryParam("lng", longitude)
                          .queryParam("rad", DEFAULT_RADIUS_KM)
                          .queryParam("sort", DEFAULT_SORT)
                          .queryParam("type", DEFAULT_TYPE)
                          .queryParam("apikey", apiKey)
                          .build())
              .accept(MediaType.APPLICATION_JSON)
              .retrieve()
              .body(String.class);
    } catch (RestClientException exception) {
      // Upstream client exceptions can include the full URI. Avoid preserving that chain because
      // Tankerkönig requires the API key in the query string.
      throw new FuelPriceLookupException("Tankerkönig request failed.");
    }

    return parseStationResponse(responseBody);
  }

  // Parse manually instead of binding directly so the backend can validate Tankerkönig's optional
  // and shape-shifting fields before they become trusted application data.
  private static List<GasStation> parseStationResponse(String responseBody) {
    if (responseBody == null || responseBody.isBlank()) {
      throw new FuelPriceLookupException("Tankerkönig response body is missing.");
    }

    final Map<String, Object> payload;
    try {
      payload = JSON_PARSER.parseMap(responseBody);
    } catch (RuntimeException exception) {
      throw new FuelPriceLookupException("Tankerkönig response body is invalid.", exception);
    }

    if (!asBoolean(payload.get("ok"), "ok")) {
      throw new FuelPriceLookupException("Tankerkönig returned ok=false.");
    }

    final Object stationsNode = payload.get("stations");
    if (!(stationsNode instanceof List<?> stationsPayload)) {
      throw new FuelPriceLookupException("Tankerkönig stations payload is invalid.");
    }

    final List<GasStation> stations = new ArrayList<>();
    for (Object stationPayload : stationsPayload) {
      if (!(stationPayload instanceof Map<?, ?> stationNode)) {
        throw new FuelPriceLookupException("Tankerkönig station row is invalid.");
      }

      stations.add(parseStation(stationNode));
    }
    return List.copyOf(stations);
  }

  // The backend keeps only the fields needed for the current station-list view and drops the rest
  // of the upstream payload here.
  private static GasStation parseStation(Map<?, ?> stationNode) {
    final String id = requireText(stationNode, "id");
    final String name = requireText(stationNode, "name");

    return new GasStation(
        id,
        name,
        nullableText(stationNode, "brand"),
        nullableText(stationNode, "street"),
        nullableText(stationNode, "houseNumber"),
        nullablePostCode(stationNode),
        nullableText(stationNode, "place"),
        requireNumber(stationNode, "lat").doubleValue(),
        requireNumber(stationNode, "lng").doubleValue(),
        nullableNumber(stationNode, "dist"),
        nullableBoolean(stationNode, "isOpen"),
        nullablePrice(stationNode, "e5"),
        nullablePrice(stationNode, "e10"),
        nullablePrice(stationNode, "diesel"));
  }

  private static String requireText(Map<?, ?> parentNode, String fieldName) {
    final Object field = parentNode.get(fieldName);
    if (!(field instanceof String value) || value.isBlank()) {
      throw new FuelPriceLookupException("Tankerkönig field '%s' is invalid.".formatted(fieldName));
    }

    return value;
  }

  private static String nullableText(Map<?, ?> parentNode, String fieldName) {
    final Object field = parentNode.get(fieldName);
    if (field == null) {
      return null;
    }

    final String value;
    if (field instanceof String stringValue) {
      value = stringValue;
    } else if (field instanceof Number numberValue) {
      value = numberValue.toString();
    } else {
      throw new FuelPriceLookupException("Tankerkönig field '%s' is invalid.".formatted(fieldName));
    }

    final String trimmedValue = value.trim();
    return trimmedValue.isEmpty() ? null : trimmedValue;
  }

  // Tankerkönig can send postcodes as JSON numbers. Preserve German leading zeros instead of
  // routing that path through generic Number.toString().
  private static String nullablePostCode(Map<?, ?> stationNode) {
    final Object field = stationNode.get("postCode");
    if (field == null) {
      return null;
    }
    if (field instanceof String stringValue) {
      final String trimmedValue = stringValue.trim();
      return trimmedValue.isEmpty() ? null : trimmedValue;
    }
    if (!(field instanceof Number numberValue)) {
      throw new FuelPriceLookupException("Tankerkönig field 'postCode' is invalid.");
    }

    final BigDecimal decimalPostCode = new BigDecimal(numberValue.toString());
    if (decimalPostCode.scale() > 0 && decimalPostCode.stripTrailingZeros().scale() > 0) {
      throw new FuelPriceLookupException("Tankerkönig field 'postCode' is invalid.");
    }

    final BigInteger integralPostCode = decimalPostCode.toBigIntegerExact();
    if (integralPostCode.signum() < 0
        || integralPostCode.compareTo(BigInteger.valueOf(99_999)) > 0) {
      throw new FuelPriceLookupException("Tankerkönig field 'postCode' is invalid.");
    }

    return "%05d".formatted(integralPostCode.longValueExact());
  }

  private static BigDecimal requireNumber(Map<?, ?> parentNode, String fieldName) {
    final BigDecimal value = nullableNumber(parentNode, fieldName);
    if (value == null) {
      throw new FuelPriceLookupException("Tankerkönig field '%s' is invalid.".formatted(fieldName));
    }
    return value;
  }

  private static BigDecimal nullableNumber(Map<?, ?> parentNode, String fieldName) {
    final Object field = parentNode.get(fieldName);
    if (field == null) {
      return null;
    }
    if (!(field instanceof Number number)) {
      throw new FuelPriceLookupException("Tankerkönig field '%s' is invalid.".formatted(fieldName));
    }

    return new BigDecimal(number.toString());
  }

  private static Boolean nullableBoolean(Map<?, ?> parentNode, String fieldName) {
    final Object field = parentNode.get(fieldName);
    if (field == null) {
      return null;
    }
    if (!(field instanceof Boolean value)) {
      throw new FuelPriceLookupException("Tankerkönig field '%s' is invalid.".formatted(fieldName));
    }

    return value;
  }

  // Tankerkönig can encode unsupported fuel types as `false` instead of a numeric price.
  private static BigDecimal nullablePrice(Map<?, ?> parentNode, String fieldName) {
    final Object field = parentNode.get(fieldName);
    if (field == null) {
      return null;
    }
    if (field instanceof Boolean value) {
      if (!value) {
        return null;
      }

      throw new FuelPriceLookupException("Tankerkönig field '%s' is invalid.".formatted(fieldName));
    }
    if (!(field instanceof Number number)) {
      throw new FuelPriceLookupException("Tankerkönig field '%s' is invalid.".formatted(fieldName));
    }

    return new BigDecimal(number.toString());
  }

  private static boolean asBoolean(Object field, String fieldName) {
    if (field instanceof Boolean value) {
      return value;
    }

    throw new FuelPriceLookupException("Tankerkönig field '%s' is invalid.".formatted(fieldName));
  }
}
