package io.github.jantrw.carfuellive.tankerkoenig.service;

import io.github.jantrw.carfuellive.stations.model.GasStation;
import java.util.List;

/**
 * Backend abstraction for retrieving nearby stations from Tankerkönig.
 *
 * <p>The current implementation calls the public REST API, while the rest of the backend depends
 * only on this normalized station-search contract.
 */
public interface TankerkoenigStationSearchClient {

  List<GasStation> searchStations(double latitude, double longitude);
}
