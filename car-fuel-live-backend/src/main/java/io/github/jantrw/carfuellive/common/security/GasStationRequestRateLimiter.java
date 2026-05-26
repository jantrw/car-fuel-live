package io.github.jantrw.carfuellive.common.security;

/**
 * Minimal abstraction for fuel-price request throttling.
 *
 * <p>The interface keeps the filter independent from the concrete limiter implementation so the
 * current in-memory strategy can later be replaced without touching request handling.
 */
public interface GasStationRequestRateLimiter {

  boolean allowRequest(String clientAddress);
}
