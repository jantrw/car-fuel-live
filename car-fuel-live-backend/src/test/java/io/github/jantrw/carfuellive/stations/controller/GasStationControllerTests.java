package io.github.jantrw.carfuellive.stations.controller;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.jantrw.carfuellive.common.exception.FuelPriceLookupException;
import io.github.jantrw.carfuellive.common.security.InMemoryGasStationRequestRateLimiter;
import io.github.jantrw.carfuellive.stations.model.GasStation;
import io.github.jantrw.carfuellive.tankerkoenig.service.TankerkoenigRestClient;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.security.trusted-proxies[0]=127.0.0.1/32")
@AutoConfigureMockMvc
class GasStationControllerTests {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private TankerkoenigRestClient tankerkoenigRestClient;
  @MockitoBean private InMemoryGasStationRequestRateLimiter gasStationRequestRateLimiter;

  @BeforeEach
  void allowRequestsByDefault() {
    when(gasStationRequestRateLimiter.allowRequest(anyString())).thenReturn(true);
  }

  @Test
  void should_returnStationsWithPrices_when_validCoordinatesProvided() throws Exception {
    when(tankerkoenigRestClient.searchStations(52.52437, 13.41053))
        .thenReturn(
            List.of(
                new GasStation(
                    "station-1",
                    "Fuel Stop",
                    "Brand",
                    "Main Street",
                    "10",
                    "10115",
                    "Berlin",
                    52.52,
                    13.41,
                    new BigDecimal("0.5"),
                    true,
                    new BigDecimal("1.759"),
                    new BigDecimal("1.699"),
                    new BigDecimal("1.589"))));

    mockMvc
        .perform(get("/api/v1/gas-stations").param("lat", "52.52437").param("lng", "13.41053"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value("station-1"))
        .andExpect(jsonPath("$.items[0].name").value("Fuel Stop"))
        .andExpect(jsonPath("$.items[0].e5").value(1.759))
        .andExpect(jsonPath("$.items[0].diesel").value(1.589));
  }

  @Test
  void should_returnValidationError_when_latitudeIsOutOfRange() throws Exception {
    mockMvc
        .perform(get("/api/v1/gas-stations").param("lat", "91").param("lng", "13.41053"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("latitude"));
  }

  @Test
  void should_returnUpstreamError_when_fuelPriceLookupFails() throws Exception {
    when(tankerkoenigRestClient.searchStations(anyDouble(), anyDouble()))
        .thenThrow(new FuelPriceLookupException("Upstream failed."));

    mockMvc
        .perform(get("/api/v1/gas-stations").param("lat", "52.52437").param("lng", "13.41053"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.code").value("UPSTREAM_ERROR"));
  }

  @Test
  void should_returnTooManyRequests_when_rateLimitIsExceeded() throws Exception {
    when(gasStationRequestRateLimiter.allowRequest(anyString())).thenReturn(false);

    mockMvc
        .perform(get("/api/v1/gas-stations").param("lat", "52.52437").param("lng", "13.41053"))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.code").value("RATE_LIMITED"));

    verifyNoInteractions(tankerkoenigRestClient);
  }

  @Test
  void should_useForwardedClientAddress_when_rateLimitRunsBehindTrustedProxy() throws Exception {
    when(gasStationRequestRateLimiter.allowRequest("198.51.100.7")).thenReturn(false);

    mockMvc
        .perform(
            get("/api/v1/gas-stations")
                .param("lat", "52.52437")
                .param("lng", "13.41053")
                .header("X-Forwarded-For", "198.51.100.7, 127.0.0.1")
                .with(
                    request -> {
                      request.setRemoteAddr("127.0.0.1");
                      return request;
                    }))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.code").value("RATE_LIMITED"));

    verify(gasStationRequestRateLimiter).allowRequest("198.51.100.7");
    verifyNoInteractions(tankerkoenigRestClient);
  }
}
