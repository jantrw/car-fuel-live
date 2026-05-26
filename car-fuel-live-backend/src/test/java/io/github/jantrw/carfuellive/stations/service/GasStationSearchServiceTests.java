package io.github.jantrw.carfuellive.stations.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.github.jantrw.carfuellive.common.exception.FuelPriceLookupException;
import io.github.jantrw.carfuellive.stations.model.GasStation;
import io.github.jantrw.carfuellive.tankerkoenig.service.TankerkoenigStationSearchClient;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class GasStationSearchServiceTests {

  @Test
  void should_returnMappedStations_when_validCoordinatesProvided() {
    final TankerkoenigStationSearchClient client =
        (latitude, longitude) ->
            List.of(
                new GasStation(
                    "station-1",
                    "Fuel Stop",
                    "Brand",
                    "Main Street",
                    "10",
                    "10115",
                    "Berlin",
                    latitude,
                    longitude,
                    new BigDecimal("0.5"),
                    true,
                    new BigDecimal("1.759"),
                    new BigDecimal("1.699"),
                    new BigDecimal("1.589")));
    final GasStationSearchService service = new GasStationSearchService(client);

    final var response = service.searchStations(52.52437, 13.41053);

    assertEquals(1, response.items().size());
    assertEquals("station-1", response.items().getFirst().id());
    assertEquals("Fuel Stop", response.items().getFirst().name());
    assertEquals(new BigDecimal("1.759"), response.items().getFirst().e5());
  }

  @Test
  void should_deduplicateInflightRequests_when_sameCoordinatesArriveConcurrently()
      throws Exception {
    final AtomicInteger requestCount = new AtomicInteger();
    final CountDownLatch firstRequestStarted = new CountDownLatch(1);
    final CountDownLatch releaseFirstRequest = new CountDownLatch(1);
    final TankerkoenigStationSearchClient client =
        (latitude, longitude) -> {
          requestCount.incrementAndGet();
          firstRequestStarted.countDown();
          try {
            if (!releaseFirstRequest.await(2, TimeUnit.SECONDS)) {
              throw new IllegalStateException("Timed out while waiting for test release.");
            }
          } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                "Interrupted while waiting for test release.", exception);
          }

          return List.of(
              new GasStation(
                  "station-1",
                  "Fuel Stop",
                  null,
                  null,
                  null,
                  null,
                  null,
                  latitude,
                  longitude,
                  new BigDecimal("0.5"),
                  true,
                  new BigDecimal("1.759"),
                  null,
                  new BigDecimal("1.589")));
        };
    final GasStationSearchService service = new GasStationSearchService(client);

    try (var executor = Executors.newFixedThreadPool(2)) {
      final Callable<String> searchTask =
          () -> service.searchStations(52.52437, 13.41053).items().getFirst().id();

      final Future<String> firstResult = executor.submit(searchTask);
      firstRequestStarted.await(2, TimeUnit.SECONDS);
      final Future<String> secondResult = executor.submit(searchTask);

      releaseFirstRequest.countDown();

      assertEquals("station-1", firstResult.get(2, TimeUnit.SECONDS));
      assertEquals("station-1", secondResult.get(2, TimeUnit.SECONDS));
      assertEquals(1, requestCount.get());
    }
  }

  @Test
  void should_propagateFuelPriceLookupException_when_deduplicatedRequestFails() throws Exception {
    final CountDownLatch firstRequestStarted = new CountDownLatch(1);
    final CountDownLatch releaseFailure = new CountDownLatch(1);
    final TankerkoenigStationSearchClient client =
        (latitude, longitude) -> {
          firstRequestStarted.countDown();
          try {
            if (!releaseFailure.await(2, TimeUnit.SECONDS)) {
              throw new IllegalStateException("Timed out while waiting for test release.");
            }
          } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                "Interrupted while waiting for test release.", exception);
          }

          throw new FuelPriceLookupException("Upstream failed.");
        };
    final GasStationSearchService service = new GasStationSearchService(client);

    try (var executor = Executors.newFixedThreadPool(2)) {
      final Callable<Void> searchTask =
          () -> {
            service.searchStations(52.52437, 13.41053);
            return null;
          };

      final Future<Void> firstResult = executor.submit(searchTask);
      firstRequestStarted.await(2, TimeUnit.SECONDS);
      final Future<Void> secondResult = executor.submit(searchTask);

      releaseFailure.countDown();

      assertFuelPriceLookupFailure(firstResult);
      assertFuelPriceLookupFailure(secondResult);
    }
  }

  private static void assertFuelPriceLookupFailure(Future<Void> result) throws Exception {
    try {
      result.get(2, TimeUnit.SECONDS);
    } catch (ExecutionException exception) {
      assertInstanceOf(FuelPriceLookupException.class, exception.getCause());
      return;
    }

    throw new AssertionError("Expected the request to fail with FuelPriceLookupException.");
  }
}
