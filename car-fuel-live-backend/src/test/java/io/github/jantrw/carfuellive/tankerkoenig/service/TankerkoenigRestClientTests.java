package io.github.jantrw.carfuellive.tankerkoenig.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.github.jantrw.carfuellive.common.exception.FuelPriceLookupException;
import io.github.jantrw.carfuellive.tankerkoenig.config.TankerkoenigProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TankerkoenigRestClientTests {

  @Test
  void should_returnStations_when_upstreamPayloadIsValid() {
    final TankerkoenigProperties properties = tankerkoenigProperties();

    final RestClient.Builder builder = RestClient.builder();
    final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    final TankerkoenigRestClient client =
        new TankerkoenigRestClient(builder.baseUrl(properties.baseUrl()).build(), properties);

    server
        .expect(requestTo(org.hamcrest.Matchers.containsString("/json/list.php")))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                """
                {
                  "ok": true,
                  "stations": [
                    {
                      "id": "station-1",
                      "name": "Fuel Stop",
                      "brand": "Brand",
                      "street": "Main Street",
                      "houseNumber": "10",
                      "postCode": "10115",
                      "place": "Berlin",
                      "lat": 52.52,
                      "lng": 13.41,
                      "dist": 0.5,
                      "isOpen": true,
                      "e5": 1.759,
                      "e10": false,
                      "diesel": 1.589
                    }
                  ]
                }
                """,
                MediaType.APPLICATION_JSON));

    final List<io.github.jantrw.carfuellive.stations.model.GasStation> stations =
        client.searchStations(52.52437, 13.41053);

    assertEquals(1, stations.size());
    assertEquals("station-1", stations.getFirst().id());
    assertEquals("Fuel Stop", stations.getFirst().name());
    assertNull(stations.getFirst().e10());
    server.verify();
  }

  @Test
  void should_preserveLeadingZero_when_upstreamPostCodeIsNumeric() {
    final TankerkoenigProperties properties = tankerkoenigProperties();

    final RestClient.Builder builder = RestClient.builder();
    final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    final TankerkoenigRestClient client =
        new TankerkoenigRestClient(builder.baseUrl(properties.baseUrl()).build(), properties);

    server
        .expect(requestTo(org.hamcrest.Matchers.containsString("/json/list.php")))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                """
                {
                  "ok": true,
                  "stations": [
                    {
                      "id": "station-2",
                      "name": "Altstadt Fuel",
                      "postCode": 1067,
                      "place": "Dresden",
                      "lat": 51.0504,
                      "lng": 13.7373,
                      "e5": 1.799,
                      "e10": 1.739,
                      "diesel": 1.629
                    }
                  ]
                }
                """,
                MediaType.APPLICATION_JSON));

    final List<io.github.jantrw.carfuellive.stations.model.GasStation> stations =
        client.searchStations(51.0504, 13.7373);

    assertEquals("01067", stations.getFirst().postCode());
    server.verify();
  }

  @Test
  void should_throwFuelPriceLookupException_when_upstreamReportsFailure() {
    final TankerkoenigProperties properties = tankerkoenigProperties();

    final RestClient.Builder builder = RestClient.builder();
    final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    final TankerkoenigRestClient client =
        new TankerkoenigRestClient(builder.baseUrl(properties.baseUrl()).build(), properties);

    server
        .expect(requestTo(org.hamcrest.Matchers.containsString("/json/list.php")))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                """
                {
                  "ok": false,
                  "message": "temporary error"
                }
                """,
                MediaType.APPLICATION_JSON));

    assertThrows(FuelPriceLookupException.class, () -> client.searchStations(52.52437, 13.41053));
    server.verify();
  }

  @Test
  void should_dropUpstreamCause_when_httpClientFails() {
    final TankerkoenigProperties properties = tankerkoenigProperties();

    final RestClient.Builder builder = RestClient.builder();
    final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    final TankerkoenigRestClient client =
        new TankerkoenigRestClient(builder.baseUrl(properties.baseUrl()).build(), properties);

    server
        .expect(requestTo(org.hamcrest.Matchers.containsString("/json/list.php")))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withServerError());

    final FuelPriceLookupException exception =
        assertThrows(
            FuelPriceLookupException.class, () -> client.searchStations(52.52437, 13.41053));

    assertEquals("Tankerkönig request failed.", exception.getMessage());
    assertNull(exception.getCause());
    server.verify();
  }

  private static TankerkoenigProperties tankerkoenigProperties() {
    return new TankerkoenigProperties(
        "https://example.test", "test-key", Duration.ofSeconds(3), Duration.ofSeconds(5));
  }
}
