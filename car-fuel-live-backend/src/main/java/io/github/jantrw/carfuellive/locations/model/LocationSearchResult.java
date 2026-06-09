package io.github.jantrw.carfuellive.locations.model;

/**
 * Internal search row used while composing location suggestions.
 *
 * <p>The repository assembles this record from country, place, alias, and postal-code queries.
 * Besides the visible label and coordinates it carries ranking and administrative metadata that the
 * service needs for sorting and semantic deduplication.
 */
public record LocationSearchResult(
    String type,
    String id,
    String label,
    String countryCode,
    Double latitude,
    Double longitude,
    String postalCode,
    String featureClass,
    String admin1Code,
    String admin2Code,
    String admin3Code,
    String admin4Code,
    int matchRank,
    long popularity) {}
