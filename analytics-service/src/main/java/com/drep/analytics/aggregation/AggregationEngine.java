package com.drep.analytics.aggregation;

import com.drep.common.model.ProcessedEvent;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AggregationEngine {

    private final MetricValueExtractor metricValueExtractor;
    private final Map<String, AggregationBucket> activeBuckets = new ConcurrentHashMap<>();

    public AggregationEngine(MetricValueExtractor metricValueExtractor) {
        this.metricValueExtractor = metricValueExtractor;
    }

    public void ingest(ProcessedEvent event) {
        Instant eventTime = event.processedAt() != null ? event.processedAt() : event.timestamp();
        for (WindowSize window : WindowSize.all()) {
            Instant bucketStart = MetricValueExtractor.bucketStart(eventTime, window);
            Instant bucketEnd = MetricValueExtractor.bucketEnd(bucketStart, window);
            String key = event.tenantId() + ":" + event.eventType() + ":" + window.code() + ":" + bucketStart.toEpochMilli();

            AggregationBucket bucket = activeBuckets.computeIfAbsent(key,
                    k -> new AggregationBucket(event.tenantId(), event.eventType(), window, bucketStart, bucketEnd));

            metricValueExtractor.extract(event).ifPresentOrElse(
                    bucket::record,
                    bucket::recordCountOnly
            );
        }
    }

    public List<AggregationBucket> flushCompletedBuckets(Instant now) {
        List<AggregationBucket> completed = new ArrayList<>();
        for (Map.Entry<String, AggregationBucket> entry : activeBuckets.entrySet()) {
            if (!entry.getValue().getBucketEnd().isAfter(now)) {
                completed.add(entry.getValue());
                activeBuckets.remove(entry.getKey());
            }
        }
        return completed;
    }

    public List<AggregationBucket> flushAll() {
        List<AggregationBucket> all = new ArrayList<>(activeBuckets.values());
        activeBuckets.clear();
        return all;
    }
}
