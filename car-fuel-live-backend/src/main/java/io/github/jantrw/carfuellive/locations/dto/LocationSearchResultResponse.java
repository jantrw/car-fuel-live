package io.github.jantrw.carfuellive.locations.dto;

/**
 * Public suggestion item returned to the frontend.
 *
 * <p>The record is the trimmed projection of {@code LocationSearchResult}: it keeps only the data
 * needed for display, country-context updates, and optional coordinate-based follow-up actions.
 */
public record LocationSearchResultResponse(
    String type,
    String id,
    String label,
    String countryCode,
    Double latitude,
    Double longitude,
    String postalCode,
    boolean directResolution) {}
