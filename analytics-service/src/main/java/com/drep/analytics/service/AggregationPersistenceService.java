package com.drep.analytics.service;

import com.drep.analytics.aggregation.AggregationBucket;
import com.drep.analytics.config.AnalyticsProperties;
import com.drep.analytics.domain.AggregationMetricEntity;
import com.drep.analytics.repository.AggregationMetricRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AggregationPersistenceService {

    private final AggregationMetricRepository repository;
    private final AnalyticsCacheService cacheService;
    private final AlertRuleEngine alertRuleEngine;

    public AggregationPersistenceService(AggregationMetricRepository repository,
                                         AnalyticsCacheService cacheService,
                                         AlertRuleEngine alertRuleEngine) {
        this.repository = repository;
        this.cacheService = cacheService;
        this.alertRuleEngine = alertRuleEngine;
    }

    @Transactional
    public void persistBuckets(List<AggregationBucket> buckets) {
        for (AggregationBucket bucket : buckets) {
            AggregationMetricEntity entity = toEntity(bucket);
            repository.save(entity);
            cacheService.cacheMetric(entity);
            alertRuleEngine.evaluate(entity);
        }
    }

    private AggregationMetricEntity toEntity(AggregationBucket bucket) {
        AggregationMetricEntity entity = new AggregationMetricEntity();
        entity.setBucketStart(bucket.getBucketStart());
        entity.setBucketEnd(bucket.getBucketEnd());
        entity.setWindowSize(bucket.getWindowSize().code());
        entity.setTenantId(bucket.getTenantId());
        entity.setEventType(bucket.getEventType());
        entity.setEventCount(bucket.getCount());
        entity.setSumValue(bucket.getSum());
        entity.setAvgValue(bucket.getAvg());
        entity.setMinValue(bucket.getMin());
        entity.setMaxValue(bucket.getMax());
        entity.setP50(bucket.getP50());
        entity.setP95(bucket.getP95());
        entity.setP99(bucket.getP99());
        entity.setUpdatedAt(Instant.now());
        return entity;
    }
}
