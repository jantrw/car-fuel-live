package io.github.jantrw.carfuellive.common.exception;

/**
 * Field-specific validation detail inside {@link ApiErrorResponse}.
 *
 * <p>It points the client to the exact request field that failed validation without leaking backend
 * implementation details.
 */
public record FieldErrorResponse(String field, String message) {}
