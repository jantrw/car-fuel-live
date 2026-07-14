package io.github.jantrw.carfuellive.stations.dto;

import io.github.jantrw.carfuellive.stations.model.GasStation;
import java.util.List;

/**
 * Public response wrapper for live fuel-price station lookups.
 *
 * <p>The payload stays intentionally narrow so the frontend can render nearby stations without
 * inheriting Tankerkönig's raw response shape.
 */
public record GasStationSearchResponse(List<GasStation> items) {}
