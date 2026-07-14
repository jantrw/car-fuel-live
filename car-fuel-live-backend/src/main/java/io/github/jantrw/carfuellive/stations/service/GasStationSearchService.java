package io.github.jantrw.carfuellive.stations.service;

import io.github.jantrw.carfuellive.stations.dto.GasStationSearchResponse;
import io.github.jantrw.carfuellive.tankerkoenig.service.TankerkoenigRestClient;
import org.springframework.stereotype.Service;

@Service
/** Loads live nearby stations and projects them onto the public API response. */
public class GasStationSearchService {

  private final TankerkoenigRestClient tankerkoenigRestClient;

  public GasStationSearchService(TankerkoenigRestClient tankerkoenigRestClient) {
    this.tankerkoenigRestClient = tankerkoenigRestClient;
  }

  public GasStationSearchResponse searchStations(double latitude, double longitude) {
    return new GasStationSearchResponse(tankerkoenigRestClient.searchStations(latitude, longitude));
  }
}
