package io.github.jantrw.carfuellive.stations.controller;

import io.github.jantrw.carfuellive.stations.dto.GasStationSearchResponse;
import io.github.jantrw.carfuellive.stations.service.GasStationSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP entry point for live fuel-price lookups around coordinates.
 *
 * <p>The controller keeps only request validation and OpenAPI metadata; the Tankerkönig-backed
 * lookup flow itself lives in {@link GasStationSearchService}.
 */
@RestController
@RequestMapping("/api/v1/gas-stations")
@Validated
public class GasStationController {

  private final GasStationSearchService gasStationSearchService;

  public GasStationController(GasStationSearchService gasStationSearchService) {
    this.gasStationSearchService = gasStationSearchService;
  }

  @Operation(
      summary = "Search gas stations by coordinates",
      description =
          "Resolve gas stations and live fuel prices around coordinates through the Tankerkönig backend integration.")
  @ApiResponse(responseCode = "200", description = "Gas station lookup completed.")
  @ApiResponse(responseCode = "400", description = "Gas station lookup request validation failed.")
  @ApiResponse(responseCode = "502", description = "Fuel price service is currently unavailable.")
  @GetMapping
  GasStationSearchResponse searchGasStations(
      @RequestParam("lat")
          @DecimalMin(value = "-90.0", message = "Latitude must be at least -90.")
          @DecimalMax(value = "90.0", message = "Latitude must be at most 90.")
          double latitude,
      @RequestParam("lng")
          @DecimalMin(value = "-180.0", message = "Longitude must be at least -180.")
          @DecimalMax(value = "180.0", message = "Longitude must be at most 180.")
          double longitude) {
    return gasStationSearchService.searchStations(latitude, longitude);
  }
}
