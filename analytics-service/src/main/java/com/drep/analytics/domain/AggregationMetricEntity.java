package com.drep.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "aggregation_metrics")
@IdClass(AggregationMetricEntity.AggregationMetricKey.class)
public class AggregationMetricEntity {

    @Id
    @Column(name = "bucket_start")
    private Instant bucketStart;

    @Id
    @Column(name = "window_size")
    private String windowSize;

    @Id
    @Column(name = "tenant_id")
    private String tenantId;

    @Id
    @Column(name = "event_type")
    private String eventType;

    @Column(name = "bucket_end", nullable = false)
    private Instant bucketEnd;

    @Column(name = "event_count", nullable = false)
    private long eventCount;

    private Double sumValue;
    private Double avgValue;
    private Double minValue;
    private Double maxValue;
    private Double p50;
    private Double p95;
    private Double p99;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Instant getBucketStart() {
        return bucketStart;
    }

    public void setBucketStart(Instant bucketStart) {
        this.bucketStart = bucketStart;
    }

    public Instant getBucketEnd() {
        return bucketEnd;
    }

    public void setBucketEnd(Instant bucketEnd) {
        this.bucketEnd = bucketEnd;
    }

    public String getWindowSize() {
        return windowSize;
    }

    public void setWindowSize(String windowSize) {
        this.windowSize = windowSize;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public long getEventCount() {
        return eventCount;
    }

    public void setEventCount(long eventCount) {
        this.eventCount = eventCount;
    }

    public Double getSumValue() {
        return sumValue;
    }

    public void setSumValue(Double sumValue) {
        this.sumValue = sumValue;
    }

    public Double getAvgValue() {
        return avgValue;
    }

    public void setAvgValue(Double avgValue) {
        this.avgValue = avgValue;
    }

    public Double getMinValue() {
        return minValue;
    }

    public void setMinValue(Double minValue) {
        this.minValue = minValue;
    }

    public Double getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(Double maxValue) {
        this.maxValue = maxValue;
    }

    public Double getP50() {
        return p50;
    }

    public void setP50(Double p50) {
        this.p50 = p50;
    }

    public Double getP95() {
        return p95;
    }

    public void setP95(Double p95) {
        this.p95 = p95;
    }

    public Double getP99() {
        return p99;
    }

    public void setP99(Double p99) {
        this.p99 = p99;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static class AggregationMetricKey implements Serializable {
        private Instant bucketStart;
        private String windowSize;
        private String tenantId;
        private String eventType;

        public AggregationMetricKey() {
        }

        public AggregationMetricKey(Instant bucketStart, String windowSize, String tenantId, String eventType) {
            this.bucketStart = bucketStart;
            this.windowSize = windowSize;
            this.tenantId = tenantId;
            this.eventType = eventType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof AggregationMetricKey that)) {
                return false;
            }
            return Objects.equals(bucketStart, that.bucketStart)
                    && Objects.equals(windowSize, that.windowSize)
                    && Objects.equals(tenantId, that.tenantId)
                    && Objects.equals(eventType, that.eventType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(bucketStart, windowSize, tenantId, eventType);
        }
    }
}
