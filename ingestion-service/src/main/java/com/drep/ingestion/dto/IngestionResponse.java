package com.drep.ingestion.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record IngestionResponse(
        String status,
        String message,
        String traceId,
        int acceptedCount,
        List<String> eventIds,
        Instant acceptedAt
) {
    public static IngestionResponse accepted(String traceId, int count, List<String> eventIds) {
        return new IngestionResponse(
                "ACCEPTED",
                "Events queued for async processing",
                traceId,
                count,
                eventIds,
                Instant.now()
        );
    }
}
