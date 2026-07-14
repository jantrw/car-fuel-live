package io.github.jantrw.carfuellive.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Applies lightweight request throttling to the live fuel-price endpoint.
 *
 * <p>The filter only guards {@code GET /api/v1/gas-stations}; other routes stay untouched so
 * location search and framework internals are not coupled to this first abuse-protection slice.
 */
@Component
public class GasStationRateLimitFilter extends OncePerRequestFilter {

  private static final String GAS_STATIONS_PATH = "/api/v1/gas-stations";
  private static final String RATE_LIMITED_RESPONSE_BODY =
      """
      {"code":"RATE_LIMITED","message":"Too many fuel price requests from this client.","details":[]}
      """;

  private final ClientAddressResolver clientAddressResolver;
  private final InMemoryGasStationRequestRateLimiter gasStationRequestRateLimiter;

  public GasStationRateLimitFilter(
      ClientAddressResolver clientAddressResolver,
      InMemoryGasStationRequestRateLimiter gasStationRequestRateLimiter) {
    this.clientAddressResolver = clientAddressResolver;
    this.gasStationRequestRateLimiter = gasStationRequestRateLimiter;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !HttpMethod.GET.matches(request.getMethod())
        || !GAS_STATIONS_PATH.equals(requestPath(request));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (!gasStationRequestRateLimiter.allowRequest(
        clientAddressResolver.resolveClientAddress(request))) {
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.getWriter().write(RATE_LIMITED_RESPONSE_BODY);
      return;
    }

    filterChain.doFilter(request, response);
  }

  private static String requestPath(HttpServletRequest request) {
    final String contextPath = request.getContextPath();
    final String requestUri = request.getRequestURI();
    if (contextPath.isEmpty() || !requestUri.startsWith(contextPath)) {
      return requestUri;
    }

    return requestUri.substring(contextPath.length());
  }
}
