package com.drep.analytics.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AnalyticsMetricDto(
        String tenantId,
        String eventType,
        String window,
        Instant bucketStart,
        Instant bucketEnd,
        long count,
        Double sum,
        Double avg,
        Double min,
        Double max,
        Double p50,
        Double p95,
        Double p99
) {
}
