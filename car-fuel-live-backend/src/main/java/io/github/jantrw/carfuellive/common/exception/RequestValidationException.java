package io.github.jantrw.carfuellive.common.exception;

/**
 * Runtime exception for request validation failures detected outside Bean Validation.
 *
 * <p>The stored field name lets {@link RestExceptionHandler} convert the failure into the same
 * structured error payload used by annotation-driven validation.
 */
public class RequestValidationException extends RuntimeException {

  private final String field;

  public RequestValidationException(String field, String message) {
    super(message);
    this.field = field;
  }

  public String field() {
    return field;
  }
}
