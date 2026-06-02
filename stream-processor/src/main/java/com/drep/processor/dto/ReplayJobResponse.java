package com.drep.processor.dto;

import java.time.Instant;
import java.util.UUID;

public record ReplayJobResponse(
        UUID jobId,
        String status,
        Instant startedAt,
        Instant completedAt,
        int replayedCount,
        String consumerGroup,
        String message
) {
    public static ReplayJobResponse running(UUID jobId, String consumerGroup) {
        return new ReplayJobResponse(jobId, "RUNNING", Instant.now(), null, 0, consumerGroup, null);
    }

    public static ReplayJobResponse completed(UUID jobId, String consumerGroup, int replayedCount) {
        return new ReplayJobResponse(jobId, "COMPLETED", null, Instant.now(), replayedCount, consumerGroup,
                "Replay finished");
    }

    public static ReplayJobResponse failed(UUID jobId, String consumerGroup, String message) {
        return new ReplayJobResponse(jobId, "FAILED", null, Instant.now(), 0, consumerGroup, message);
    }
}
