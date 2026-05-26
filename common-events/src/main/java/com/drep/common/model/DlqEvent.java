package com.drep.common.model;

import java.time.Instant;
import java.util.UUID;

public record DlqEvent(
        UUID dlqId,
        Event originalEvent,
        String failureReason,
        int retryCount,
        Instant failedAt,
        String sourceTopic,
        String consumerGroup
) {
    public static DlqEvent of(Event originalEvent, String failureReason, int retryCount,
                              String sourceTopic, String consumerGroup) {
        return new DlqEvent(
                UUID.randomUUID(),
                originalEvent,
                failureReason,
                retryCount,
                Instant.now(),
                sourceTopic,
                consumerGroup
        );
    }
}
