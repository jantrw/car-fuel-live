package io.github.jantrw.carfuellive.common.exception;

/**
 * Internal failure signal for Tankerkönig-backed fuel-price lookups.
 *
 * <p>Service and client code throw this exception when the upstream request, payload parsing, or
 * configuration is invalid; {@link RestExceptionHandler} then maps it to the public 502 contract.
 */
public class FuelPriceLookupException extends RuntimeException {

  public FuelPriceLookupException(String message) {
    super(message);
  }

  public FuelPriceLookupException(String message, Throwable cause) {
    super(message, cause);
  }
}
