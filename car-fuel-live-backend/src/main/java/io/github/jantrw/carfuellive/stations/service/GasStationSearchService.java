package io.github.jantrw.carfuellive.stations.service;

import io.github.jantrw.carfuellive.stations.dto.GasStationResponseItem;
import io.github.jantrw.carfuellive.stations.dto.GasStationSearchResponse;
import io.github.jantrw.carfuellive.stations.model.GasStation;
import io.github.jantrw.carfuellive.tankerkoenig.service.TankerkoenigStationSearchClient;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;

@Service
/**
 * Loads live nearby stations and projects them onto the public API response.
 *
 * <p>The service also deduplicates identical in-flight coordinate lookups per node so concurrent
 * callers share one upstream Tankerkönig request.
 */
public class GasStationSearchService {

  private final TankerkoenigStationSearchClient tankerkoenigStationSearchClient;
  private final ConcurrentMap<String, CompletableFuture<GasStationSearchResponse>>
      inFlightRequests = new ConcurrentHashMap<>();

  public GasStationSearchService(TankerkoenigStationSearchClient tankerkoenigStationSearchClient) {
    this.tankerkoenigStationSearchClient = tankerkoenigStationSearchClient;
  }

  public GasStationSearchResponse searchStations(double latitude, double longitude) {
    final String requestKey = requestKey(latitude, longitude);
    final CompletableFuture<GasStationSearchResponse> newRequest = new CompletableFuture<>();
    final CompletableFuture<GasStationSearchResponse> existingRequest =
        inFlightRequests.putIfAbsent(requestKey, newRequest);

    if (existingRequest != null) {
      return awaitExistingRequest(existingRequest);
    }

    try {
      final GasStationSearchResponse response =
          new GasStationSearchResponse(
              tankerkoenigStationSearchClient.searchStations(latitude, longitude).stream()
                  .map(GasStationSearchService::toResponseItem)
                  .toList());
      newRequest.complete(response);
      return response;
    } catch (RuntimeException exception) {
      newRequest.completeExceptionally(exception);
      throw exception;
    } finally {
      inFlightRequests.remove(requestKey, newRequest);
    }
  }

  // In-flight deduplication must preserve the original runtime exception type so the controller
  // advice can still map upstream failures consistently for concurrent followers.
  private static GasStationSearchResponse awaitExistingRequest(
      CompletableFuture<GasStationSearchResponse> existingRequest) {
    try {
      return existingRequest.join();
    } catch (CompletionException exception) {
      if (exception.getCause() instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }

      throw exception;
    }
  }

  // The public DTO currently stays field-for-field aligned with the backend station model, but
  // the explicit projection keeps the API boundary decoupled from future internal model changes.
  private static GasStationResponseItem toResponseItem(GasStation gasStation) {
    return new GasStationResponseItem(
        gasStation.id(),
        gasStation.name(),
        gasStation.brand(),
        gasStation.street(),
        gasStation.houseNumber(),
        gasStation.postCode(),
        gasStation.place(),
        gasStation.latitude(),
        gasStation.longitude(),
        gasStation.distanceKm(),
        gasStation.isOpen(),
        gasStation.e5(),
        gasStation.e10(),
        gasStation.diesel());
  }

  // Coordinates are already validated at the controller boundary, so their string form is enough
  // to identify duplicate in-flight lookups.
  private static String requestKey(double latitude, double longitude) {
    return "%s:%s".formatted(Double.toString(latitude), Double.toString(longitude));
  }
}
