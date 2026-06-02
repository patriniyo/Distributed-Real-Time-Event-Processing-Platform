package com.drep.analytics.aggregation;

import com.drep.common.model.ProcessedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AggregationEngineTest {

    private AggregationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new AggregationEngine(new MetricValueExtractor());
    }

    @Test
    void aggregatesEventsIntoTumblingWindows() {
        ProcessedEvent event = new ProcessedEvent(
                UUID.randomUUID(),
                "order.placed",
                Instant.now(),
                "tenant-a",
                new ObjectMapper().createObjectNode().put("amount", 42.5),
                "trace-1",
                Instant.now().minus(1, ChronoUnit.MINUTES),
                Instant.parse("2026-05-24T12:01:30Z"),
                1,
                null
        );

        engine.ingest(event);
        List<AggregationBucket> flushed = engine.flushAll();

        assertThat(flushed).hasSize(3);
        AggregationBucket oneMinute = flushed.stream()
                .filter(b -> b.getWindowSize() == WindowSize.ONE_MINUTE)
                .findFirst()
                .orElseThrow();
        assertThat(oneMinute.getCount()).isEqualTo(1);
        assertThat(oneMinute.getSum()).isEqualTo(42.5);
        assertThat(oneMinute.getAvg()).isEqualTo(42.5);
        assertThat(oneMinute.getBucketStart()).isEqualTo(Instant.parse("2026-05-24T12:01:00Z"));
    }

    @Test
    void flushCompletedBucketsOnlyReturnsClosedWindows() {
        Instant now = Instant.parse("2026-05-24T12:01:15Z");
        ProcessedEvent event = new ProcessedEvent(
                UUID.randomUUID(),
                "page.view",
                now,
                "tenant-a",
                null,
                "trace-1",
                now,
                now,
                1,
                null
        );
        engine.ingest(event);

        List<AggregationBucket> completed = engine.flushCompletedBuckets(now);
        assertThat(completed).isEmpty();

        List<AggregationBucket> afterWindow = engine.flushCompletedBuckets(now.plus(1, ChronoUnit.MINUTES));
        assertThat(afterWindow).isNotEmpty();
    }
}
