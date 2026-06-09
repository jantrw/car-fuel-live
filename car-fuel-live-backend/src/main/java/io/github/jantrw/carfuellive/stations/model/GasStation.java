package io.github.jantrw.carfuellive.stations.model;

import java.math.BigDecimal;

/**
 * Backend station model produced by the Tankerkönig client.
 *
 * <p>The structure matches the normalized subset of Tankerkönig {@code list.php} fields that the
 * application uses for the current live-price MVP.
 */
public record GasStation(
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
