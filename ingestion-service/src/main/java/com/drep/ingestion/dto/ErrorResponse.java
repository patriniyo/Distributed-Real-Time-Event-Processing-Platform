package com.drep.ingestion.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String error,
        String message,
        String traceId,
        Instant timestamp,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {
    }

    public static ErrorResponse of(String error, String message, String traceId) {
        return new ErrorResponse(error, message, traceId, Instant.now(), null);
    }

    public static ErrorResponse validation(String message, String traceId, List<FieldError> fieldErrors) {
        return new ErrorResponse("VALIDATION_ERROR", message, traceId, Instant.now(), fieldErrors);
    }
}
