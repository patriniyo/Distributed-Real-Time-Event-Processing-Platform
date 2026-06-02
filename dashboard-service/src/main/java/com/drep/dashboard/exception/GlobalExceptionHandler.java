package com.drep.dashboard.exception;

import com.drep.common.dto.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ObjectMapper objectMapper;

    public GlobalExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleStatus(ResponseStatusException ex) {
        String traceId = UUID.randomUUID().toString();
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(status.name(), ex.getReason() != null ? ex.getReason() : status.getReasonPhrase(), traceId));
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<ApiErrorResponse> handleUpstream(RestClientResponseException ex) {
        String traceId = UUID.randomUUID().toString();
        log.warn("Upstream error traceId={} status={}", traceId, ex.getStatusCode().value());
        try {
            ApiErrorResponse upstream = objectMapper.readValue(ex.getResponseBodyAsByteArray(), ApiErrorResponse.class);
            if (upstream.traceId() == null || upstream.traceId().isBlank()) {
                return ResponseEntity.status(ex.getStatusCode())
                        .body(new ApiErrorResponse(
                                upstream.error(),
                                upstream.message(),
                                traceId,
                                upstream.timestamp(),
                                upstream.fieldErrors()));
            }
            return ResponseEntity.status(ex.getStatusCode()).body(upstream);
        } catch (Exception parseError) {
            return ResponseEntity.status(ex.getStatusCode())
                    .body(ApiErrorResponse.of("UPSTREAM_ERROR", ex.getStatusText(), traceId));
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex) {
        String traceId = UUID.randomUUID().toString();
        log.error("Unhandled error traceId={}", traceId, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of("INTERNAL_ERROR", "An unexpected error occurred", traceId));
    }
}
