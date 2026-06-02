package com.drep.analytics.service;

import com.drep.analytics.config.AnalyticsProperties;
import com.drep.analytics.domain.AggregationMetricEntity;
import com.drep.analytics.dto.AnalyticsQueryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class AnalyticsCacheService {

    private static final String METRIC_PREFIX = "analytics:metric:";
    private static final String QUERY_PREFIX = "analytics:query:";

    private final StringRedisTemplate redisTemplate;
    private final AnalyticsProperties properties;
    private final ObjectMapper objectMapper;

    public AnalyticsCacheService(StringRedisTemplate redisTemplate,
                                   AnalyticsProperties properties,
                                   ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public void cacheMetric(AggregationMetricEntity entity) {
        String key = METRIC_PREFIX + entity.getTenantId() + ":" + entity.getEventType()
                + ":" + entity.getWindowSize() + ":" + entity.getBucketStart().toEpochMilli();
        redisTemplate.opsForValue().set(key, serialize(entity), ttl());
    }

    public Optional<AggregationMetricEntity> getCachedMetric(String tenantId, String eventType,
                                                             String windowSize, long bucketStartMs) {
        String key = METRIC_PREFIX + tenantId + ":" + eventType + ":" + windowSize + ":" + bucketStartMs;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, AggregationMetricEntity.class));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    public Optional<AnalyticsQueryResponse> getCachedQuery(String cacheKey) {
        String json = redisTemplate.opsForValue().get(QUERY_PREFIX + cacheKey);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, AnalyticsQueryResponse.class));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    public void cacheQuery(String cacheKey, AnalyticsQueryResponse response) {
        redisTemplate.opsForValue().set(QUERY_PREFIX + cacheKey, serialize(response), ttl());
    }

    private Duration ttl() {
        return Duration.ofSeconds(properties.getCache().getTtlSeconds());
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize cache value", e);
        }
    }
}
