package io.github.jantrw.carfuellive.common.exception;

import java.util.List;

public record ApiErrorResponse(String code, String message, List<FieldErrorResponse> details) {}
