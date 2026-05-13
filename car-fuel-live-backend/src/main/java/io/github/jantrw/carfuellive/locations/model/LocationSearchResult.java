package io.github.jantrw.carfuellive.locations.model;

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
