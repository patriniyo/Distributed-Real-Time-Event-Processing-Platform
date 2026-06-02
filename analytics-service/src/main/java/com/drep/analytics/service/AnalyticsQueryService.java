package com.drep.analytics.service;

import com.drep.analytics.aggregation.WindowSize;
import com.drep.analytics.config.AnalyticsProperties;
import com.drep.analytics.domain.AggregationMetricEntity;
import com.drep.analytics.dto.AnalyticsMetricDto;
import com.drep.analytics.dto.AnalyticsQueryResponse;
import com.drep.analytics.repository.AggregationMetricRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsQueryService {

    private final AggregationMetricRepository repository;
    private final AnalyticsCacheService cacheService;
    private final com.drep.analytics.observability.AnalyticsMetrics analyticsMetrics;

    public AnalyticsQueryService(AggregationMetricRepository repository,
                                 AnalyticsCacheService cacheService,
                                 com.drep.analytics.observability.AnalyticsMetrics analyticsMetrics) {
        this.repository = repository;
        this.cacheService = cacheService;
        this.analyticsMetrics = analyticsMetrics;
    }

    @Transactional(readOnly = true)
    public AnalyticsQueryResponse query(String tenantId, String eventType, String window,
                                        Instant from, Instant to, String groupBy,
                                        int page, int size) {
        WindowSize windowSize = WindowSize.fromCode(window).orElse(WindowSize.ONE_MINUTE);
        String cacheKey = tenantId + "|" + eventType + "|" + windowSize.code() + "|"
                + from + "|" + to + "|" + groupBy + "|" + page + "|" + size;

        var cached = cacheService.getCachedQuery(cacheKey);
        if (cached.isPresent()) {
            analyticsMetrics.recordCacheHit();
            return cached.get();
        }
        analyticsMetrics.recordCacheMiss();
        io.micrometer.core.instrument.Timer.Sample sample = analyticsMetrics.startDbQuery();
        try {
            AnalyticsQueryResponse response = groupBy != null && groupBy.equalsIgnoreCase("eventType")
                    ? queryGroupedByEventType(tenantId, windowSize.code(), from, to)
                    : queryFlat(tenantId, eventType, windowSize.code(), from, to, page, size);
            cacheService.cacheQuery(cacheKey, response);
            return response;
        } finally {
            analyticsMetrics.recordDbQuery(sample);
        }
    }

    private AnalyticsQueryResponse queryFlat(String tenantId, String eventType, String windowSize,
                                             Instant from, Instant to, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AggregationMetricEntity> results = repository.findMetrics(
                tenantId, eventType, windowSize, from, to, pageable);
        List<AnalyticsMetricDto> metrics = results.getContent().stream()
                .map(this::toDto)
                .toList();
        return new AnalyticsQueryResponse(metrics, results.getTotalElements(), page, size, false);
    }

    private AnalyticsQueryResponse queryGroupedByEventType(String tenantId, String windowSize,
                                                           Instant from, Instant to) {
        List<AggregationMetricEntity> results = repository.findGroupedByEventType(
                tenantId, windowSize, from, to);
        Map<String, List<AnalyticsMetricDto>> grouped = results.stream()
                .map(this::toDto)
                .collect(Collectors.groupingBy(AnalyticsMetricDto::eventType, LinkedHashMap::new, Collectors.toList()));
        return new AnalyticsQueryResponse(null, results.size(), 0, results.size(), true, grouped);
    }

    private AnalyticsMetricDto toDto(AggregationMetricEntity entity) {
        return new AnalyticsMetricDto(
                entity.getTenantId(),
                entity.getEventType(),
                entity.getWindowSize(),
                entity.getBucketStart(),
                entity.getBucketEnd(),
                entity.getEventCount(),
                entity.getSumValue(),
                entity.getAvgValue(),
                entity.getMinValue(),
                entity.getMaxValue(),
                entity.getP50(),
                entity.getP95(),
                entity.getP99()
        );
    }
}
