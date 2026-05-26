package com.drep.common.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record Event(
        UUID eventId,
        String eventType,
        Instant timestamp,
        String tenantId,
        JsonNode payload,
        String traceId,
        Instant ingestedAt
) {
    public static Event from(String eventType, Instant timestamp, String tenantId,
                             JsonNode payload, String traceId) {
        return new Event(
                UUID.randomUUID(),
                eventType,
                timestamp,
                tenantId,
                payload,
                traceId,
                Instant.now()
        );
    }
}
