package com.drep.ingestion.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class IngestionMetrics {

    private final Counter ingestedEvents;
    private final Counter ingestionErrors;

    public IngestionMetrics(MeterRegistry registry) {
        this.ingestedEvents = Counter.builder("drep.ingestion.events.total")
                .description("Total events accepted for ingestion")
                .register(registry);
        this.ingestionErrors = Counter.builder("drep.ingestion.errors.total")
                .description("Total ingestion errors")
                .register(registry);
    }

    public void recordIngested(int count) {
        ingestedEvents.increment(count);
    }

    public void recordError() {
        ingestionErrors.increment();
    }
}
