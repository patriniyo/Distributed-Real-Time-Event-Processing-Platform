package com.drep.processor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ReplayRequest(
        @NotBlank String topic,
        String targetTopic,
        @NotNull Instant from,
        @NotNull Instant to,
        String tenantId,
        String eventType
) {
    public String resolvedTargetTopic() {
        return targetTopic != null && !targetTopic.isBlank() ? targetTopic : topic;
    }
}
