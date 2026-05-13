package io.github.jantrw.carfuellive.locations.dto;

public record LocationSearchResultResponse(
    String type,
    String id,
    String label,
    String countryCode,
    Double latitude,
    Double longitude,
    String postalCode) {}
