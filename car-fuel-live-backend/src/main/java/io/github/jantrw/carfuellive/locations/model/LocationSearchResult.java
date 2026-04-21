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
    int matchRank,
    long popularity) {}
