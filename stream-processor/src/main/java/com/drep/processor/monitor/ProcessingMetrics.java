package com.drep.processor.monitor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class ProcessingMetrics {

    private final Timer processingTimer;

    public ProcessingMetrics(MeterRegistry meterRegistry) {
        this.processingTimer = Timer.builder("drep.processing.latency")
                .description("Stream processing latency per event")
                .publishPercentiles(0.5, 0.99)
                .register(meterRegistry);
    }

    public void record(long durationNanos) {
        processingTimer.record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public Timer getProcessingTimer() {
        return processingTimer;
    }
}
