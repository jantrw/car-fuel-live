package io.github.jantrw.carfuellive.locations.dto;

import java.util.List;

/**
 * Public response wrapper for location suggestion requests.
 *
 * <p>Only the visible suggestion items are exposed; internal ranking and deduplication metadata
 * stays inside the backend.
 */
public record LocationSearchResponse(List<LocationSearchResultResponse> items) {}
