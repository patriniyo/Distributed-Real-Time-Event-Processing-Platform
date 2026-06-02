package com.drep.common.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record ProcessedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        String tenantId,
        JsonNode payload,
        String traceId,
        Instant ingestedAt,
        Instant processedAt,
        int schemaVersion,
        JsonNode enrichment
) {
    public static ProcessedEvent from(Event event, int schemaVersion, JsonNode enrichment) {
        return new ProcessedEvent(
                event.eventId(),
                event.eventType(),
                event.timestamp(),
                event.tenantId(),
                event.payload(),
                event.traceId(),
                event.ingestedAt(),
                Instant.now(),
                schemaVersion,
                enrichment
        );
    }
}
