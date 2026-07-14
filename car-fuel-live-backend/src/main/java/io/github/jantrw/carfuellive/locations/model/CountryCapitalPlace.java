package io.github.jantrw.carfuellive.locations.model;

/**
 * Stable country-to-capital mapping loaded from the seeded location dataset.
 *
 * <p>The structure comes from {@code location_countries.capital_place_geoname_id} joined to {@code
 * location_places}, so later country-driven flows can resolve a capital directly without a fuzzy
 * name lookup.
 */
public record CountryCapitalPlace(
    String countryCode,
    String countryName,
    long placeGeonameId,
    String placeName,
    double latitude,
    double longitude) {}
