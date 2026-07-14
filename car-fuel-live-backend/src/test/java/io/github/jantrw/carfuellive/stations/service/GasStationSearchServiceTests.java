package io.github.jantrw.carfuellive.stations.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.jantrw.carfuellive.common.exception.FuelPriceLookupException;
import io.github.jantrw.carfuellive.stations.model.GasStation;
import io.github.jantrw.carfuellive.tankerkoenig.service.TankerkoenigRestClient;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class GasStationSearchServiceTests {

  @Test
  void should_returnStations_when_clientReturnsStations() {
    final TankerkoenigRestClient client = mock(TankerkoenigRestClient.class);
    when(client.searchStations(52.52437, 13.41053))
        .thenReturn(
            List.of(
                new GasStation(
                    "station-1",
                    "Fuel Stop",
                    "Brand",
                    "Main Street",
                    "10",
                    "10115",
                    "Berlin",
                    52.52437,
                    13.41053,
                    new BigDecimal("0.5"),
                    true,
                    new BigDecimal("1.759"),
                    new BigDecimal("1.699"),
                    new BigDecimal("1.589"))));
    final GasStationSearchService service = new GasStationSearchService(client);

    final var response = service.searchStations(52.52437, 13.41053);

    assertEquals(1, response.items().size());
    assertEquals("station-1", response.items().getFirst().id());
    assertEquals("Fuel Stop", response.items().getFirst().name());
    assertEquals(new BigDecimal("1.759"), response.items().getFirst().e5());
  }

  @Test
  void should_propagateFuelPriceLookupException_when_clientFails() {
    final TankerkoenigRestClient client = mock(TankerkoenigRestClient.class);
    when(client.searchStations(52.52437, 13.41053))
        .thenThrow(new FuelPriceLookupException("Upstream failed."));
    final GasStationSearchService service = new GasStationSearchService(client);

    assertThrows(FuelPriceLookupException.class, () -> service.searchStations(52.52437, 13.41053));
  }
}
