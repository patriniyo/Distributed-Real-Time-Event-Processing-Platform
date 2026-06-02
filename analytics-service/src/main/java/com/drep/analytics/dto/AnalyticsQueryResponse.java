package com.drep.analytics.dto;

import java.util.List;
import java.util.Map;

public record AnalyticsQueryResponse(
        List<AnalyticsMetricDto> metrics,
        long totalElements,
        int page,
        int size,
        boolean grouped,
        Map<String, List<AnalyticsMetricDto>> groupedMetrics
) {
    public AnalyticsQueryResponse(List<AnalyticsMetricDto> metrics, long totalElements,
                                  int page, int size, boolean grouped) {
        this(metrics, totalElements, page, size, grouped, null);
    }
}
