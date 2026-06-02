package com.drep.analytics.aggregation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AggregationBucket {

    private final String tenantId;
    private final String eventType;
    private final WindowSize windowSize;
    private final Instant bucketStart;
    private final Instant bucketEnd;

    private long count;
    private double sum;
    private Double min;
    private Double max;
    private final List<Double> samples = new ArrayList<>();

    public AggregationBucket(String tenantId, String eventType, WindowSize windowSize,
                             Instant bucketStart, Instant bucketEnd) {
        this.tenantId = tenantId;
        this.eventType = eventType;
        this.windowSize = windowSize;
        this.bucketStart = bucketStart;
        this.bucketEnd = bucketEnd;
    }

    public void record(double numericValue) {
        count++;
        sum += numericValue;
        min = min == null ? numericValue : Math.min(min, numericValue);
        max = max == null ? numericValue : Math.max(max, numericValue);
        samples.add(numericValue);
    }

    public void recordCountOnly() {
        count++;
    }

    public String key() {
        return tenantId + ":" + eventType + ":" + windowSize.code() + ":" + bucketStart.toEpochMilli();
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getEventType() {
        return eventType;
    }

    public WindowSize getWindowSize() {
        return windowSize;
    }

    public Instant getBucketStart() {
        return bucketStart;
    }

    public Instant getBucketEnd() {
        return bucketEnd;
    }

    public long getCount() {
        return count;
    }

    public Double getSum() {
        return count > 0 && !samples.isEmpty() ? sum : null;
    }

    public Double getAvg() {
        return samples.isEmpty() ? null : sum / samples.size();
    }

    public Double getMin() {
        return min;
    }

    public Double getMax() {
        return max;
    }

    public Double getP50() {
        return percentile(50);
    }

    public Double getP95() {
        return percentile(95);
    }

    public Double getP99() {
        return percentile(99);
    }

    private Double percentile(double p) {
        if (samples.isEmpty()) {
            return null;
        }
        List<Double> sorted = new ArrayList<>(samples);
        Collections.sort(sorted);
        int index = (int) Math.ceil(p / 100.0 * sorted.size()) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        return sorted.get(index);
    }
}
