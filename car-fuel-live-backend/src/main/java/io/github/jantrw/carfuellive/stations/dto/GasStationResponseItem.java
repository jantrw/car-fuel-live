package io.github.jantrw.carfuellive.stations.dto;

import java.math.BigDecimal;

/**
 * Public gas-station item returned by {@code GET /api/v1/gas-stations}.
 *
 * <p>The record mirrors {@link io.github.jantrw.carfuellive.stations.model.GasStation} after the
 * backend normalizes Tankerkönig {@code list.php} station fields into the public API contract.
 */
public record GasStationResponseItem(
    String id,
    String name,
    String brand,
    String street,
    String houseNumber,
    String postCode,
    String place,
    double latitude,
    double longitude,
    BigDecimal distanceKm,
    Boolean isOpen,
    BigDecimal e5,
    BigDecimal e10,
    BigDecimal diesel) {}
