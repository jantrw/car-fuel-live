package io.github.jantrw.carfuellive.common.exception;

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
