package io.github.jantrw.carfuellive.common.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Maps backend exceptions to stable public API error responses.
 *
 * <p>This advice keeps validation failures, upstream errors, and unknown routes on one consistent
 * error contract while avoiding accidental leakage of stack traces or sensitive upstream request
 * details.
 */
@RestControllerAdvice
public class RestExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(RestExceptionHandler.class);

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
                        lastPropertySegment(violation.getPropertyPath().toString()),
                        violation.getMessage()))
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

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatch(
      MethodArgumentTypeMismatchException exception) {
    return validationError(
        new FieldErrorResponse(
            exception.getName(), "Request parameter must use the expected type."));
  }

  @ExceptionHandler(FuelPriceLookupException.class)
  ResponseEntity<ApiErrorResponse> handleFuelPriceLookupException(
      FuelPriceLookupException exception) {
    // Do not log the chained upstream exception here because HTTP client failures can include the
    // full request URI, and Tankerkönig requires the API key in the query string.
    LOGGER.warn("Fuel price lookup failed: {}", exception.getMessage());

    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(
            new ApiErrorResponse(
                "UPSTREAM_ERROR", "Fuel price service is currently unavailable.", List.of()));
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<ApiErrorResponse> handleNoResourceFound(NoResourceFoundException exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ApiErrorResponse("NOT_FOUND", "Resource was not found.", List.of()));
  }

  private ResponseEntity<ApiErrorResponse> validationError(FieldErrorResponse detail) {
    return validationError(List.of(detail));
  }

  private ResponseEntity<ApiErrorResponse> validationError(List<FieldErrorResponse> details) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ApiErrorResponse("VALIDATION_ERROR", "Request validation failed.", details));
  }

  // Constraint violation paths include the controller method prefix; clients only need the leaf
  // request field name in the error payload.
  private static String lastPropertySegment(String propertyPath) {
    final int separatorIndex = propertyPath.lastIndexOf('.');
    return separatorIndex >= 0 ? propertyPath.substring(separatorIndex + 1) : propertyPath;
  }
}
