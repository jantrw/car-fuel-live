package io.github.jantrw.carfuellive.common.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class RestExceptionHandler {

  @ExceptionHandler(RequestValidationException.class)
  ResponseEntity<ApiErrorResponse> handleRequestValidation(RequestValidationException exception) {
    return validationError(new FieldErrorResponse(exception.field(), exception.getMessage()));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  ResponseEntity<ApiErrorResponse> handleMissingRequestParameter(
      MissingServletRequestParameterException exception) {
    return validationError(
        new FieldErrorResponse(
            exception.getParameterName(), "Required request parameter is missing."));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<ApiErrorResponse> handleConstraintViolation(
      ConstraintViolationException exception) {
    final List<FieldErrorResponse> details =
        exception.getConstraintViolations().stream()
            .map(
                violation ->
                    new FieldErrorResponse(
                        violation.getPropertyPath().toString(), violation.getMessage()))
            .toList();
    return validationError(details);
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  ResponseEntity<ApiErrorResponse> handleMethodValidation(
      HandlerMethodValidationException exception) {
    final List<FieldErrorResponse> details =
        exception.getParameterValidationResults().stream()
            .flatMap(
                result ->
                    result.getResolvableErrors().stream()
                        .map(
                            error ->
                                new FieldErrorResponse(
                                    result.getMethodParameter().getParameterName(),
                                    error.getDefaultMessage())))
            .toList();
    return validationError(details);
  }

  private ResponseEntity<ApiErrorResponse> validationError(FieldErrorResponse detail) {
    return validationError(List.of(detail));
  }

  private ResponseEntity<ApiErrorResponse> validationError(List<FieldErrorResponse> details) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ApiErrorResponse("VALIDATION_ERROR", "Request validation failed.", details));
  }
}
