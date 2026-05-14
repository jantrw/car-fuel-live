package io.github.jantrw.carfuellive.locations.controller;

import io.github.jantrw.carfuellive.common.validation.TrimmedSize;
import io.github.jantrw.carfuellive.locations.dto.LocationSearchResponse;
import io.github.jantrw.carfuellive.locations.service.LocationSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/locations")
@Validated
public class LocationSearchController {

  private static final int DEFAULT_LIMIT = 8;

  private final LocationSearchService locationSearchService;

  public LocationSearchController(LocationSearchService locationSearchService) {
    this.locationSearchService = locationSearchService;
  }

  @Operation(
      summary = "Search seeded locations",
      description =
          "Search countries, places, and German postal codes from the seeded PostgreSQL dataset.")
  @ApiResponse(responseCode = "200", description = "Location search completed.")
  @ApiResponse(responseCode = "400", description = "Location search request validation failed.")
  @GetMapping("/suggestions")
  LocationSearchResponse searchLocations(
      @RequestParam("q")
          @NotBlank(message = "Query must not be blank.")
          @TrimmedSize(min = 2, max = 80, message = "Query must be between 2 and 80 characters.")
          String query,
      @RequestParam(name = "countryCode", required = false)
          @Pattern(
              regexp = "(?i)^\\s*$|^\\s*[a-z]{2}\\s*$",
              message = "Country code must be a two-letter ISO code.")
          String countryCode,
      @RequestParam(name = "limit", defaultValue = "" + DEFAULT_LIMIT)
          @Min(value = 1, message = "Limit must be at least 1.")
          @Max(value = 8, message = "Limit must be at most 8.")
          int limit) {
    return locationSearchService.suggest(query, countryCode, limit);
  }
}
