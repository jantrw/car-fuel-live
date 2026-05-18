package io.github.jantrw.carfuellive.locations.model;

public record CountryCapitalPlace(
    String countryCode,
    String countryName,
    long placeGeonameId,
    String placeName,
    double latitude,
    double longitude) {}
