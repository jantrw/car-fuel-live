package io.github.jantrw.carfuellive.common.exception;

import java.util.List;

/**
 * Public error payload returned by REST endpoints.
 *
 * <p>The shape is intentionally small: one stable error code, one user-safe message, and optional
 * field-level validation details.
 */
public record ApiErrorResponse(String code, String message, List<FieldErrorResponse> details) {}
