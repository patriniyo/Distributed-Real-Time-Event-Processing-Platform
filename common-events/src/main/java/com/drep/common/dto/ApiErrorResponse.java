package com.drep.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        String error,
        String message,
        String traceId,
        Instant timestamp,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {
    }

    public static ApiErrorResponse of(String error, String message, String traceId) {
        return new ApiErrorResponse(error, message, traceId, Instant.now(), null);
    }

    public static ApiErrorResponse validation(String message, String traceId, List<FieldError> fieldErrors) {
        return new ApiErrorResponse("VALIDATION_ERROR", message, traceId, Instant.now(), fieldErrors);
    }
}
