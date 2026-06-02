package com.drep.analytics.dto;

import java.time.Instant;
import java.util.UUID;

public record EventSummaryDto(
        UUID eventId,
        String eventType,
        String tenantId,
        Instant timestamp,
        Instant processedAt,
        String traceId,
        String payload,
        String enrichment
) {
}
