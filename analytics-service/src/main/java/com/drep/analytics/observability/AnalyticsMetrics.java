package com.drep.analytics.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsMetrics {

    private final Counter cacheHits;
    private final Counter cacheMisses;
    private final Timer dbQueryTimer;

    public AnalyticsMetrics(MeterRegistry registry) {
        this.cacheHits = Counter.builder("drep.analytics.cache.hits")
                .register(registry);
        this.cacheMisses = Counter.builder("drep.analytics.cache.misses")
                .register(registry);
        this.dbQueryTimer = Timer.builder("drep.analytics.db.query")
                .description("Analytics DB query latency")
                .register(registry);
    }

    public void recordCacheHit() {
        cacheHits.increment();
    }

    public void recordCacheMiss() {
        cacheMisses.increment();
    }

    public Timer.Sample startDbQuery() {
        return Timer.start();
    }

    public void recordDbQuery(Timer.Sample sample) {
        sample.stop(dbQueryTimer);
    }
}
