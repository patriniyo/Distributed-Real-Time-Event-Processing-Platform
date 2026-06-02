package com.drep.processor.service;

import com.drep.common.model.Event;
import com.drep.common.model.ProcessedEvent;
import com.drep.processor.config.ProcessorProperties;
import com.drep.processor.idempotency.IdempotencyService;
import com.drep.processor.monitor.ProcessingMetrics;
import com.drep.processor.pipeline.EventProcessingPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class EventProcessingService {

    private static final Logger log = LoggerFactory.getLogger(EventProcessingService.class);

    public enum Outcome { PROCESSED, DUPLICATE, FILTERED }

    public record Result(Outcome outcome, ProcessedEvent processedEvent) {
        static Result processed(ProcessedEvent event) {
            return new Result(Outcome.PROCESSED, event);
        }

        static Result duplicate() {
            return new Result(Outcome.DUPLICATE, null);
        }

        static Result filtered() {
            return new Result(Outcome.FILTERED, null);
        }
    }

    private final IdempotencyService idempotencyService;
    private final EventProcessingPipeline pipeline;
    private final ProcessedEventPublisher processedEventPublisher;
    private final ProcessorProperties processorProperties;
    private final ProcessingMetrics processingMetrics;

    public EventProcessingService(IdempotencyService idempotencyService,
                                  EventProcessingPipeline pipeline,
                                  ProcessedEventPublisher processedEventPublisher,
                                  ProcessorProperties processorProperties,
                                  ProcessingMetrics processingMetrics) {
        this.idempotencyService = idempotencyService;
        this.pipeline = pipeline;
        this.processedEventPublisher = processedEventPublisher;
        this.processorProperties = processorProperties;
        this.processingMetrics = processingMetrics;
    }

    public Result process(Event event) {
        long startNanos = System.nanoTime();
        try {
            if (processorProperties.getFailEventTypes().contains(event.eventType())) {
                throw new IllegalStateException("Forced processing failure for eventType=" + event.eventType());
            }

            if (processorProperties.getIdempotency().isEnabled()
                    && !idempotencyService.tryAcquire(event.eventId())) {
                log.info("Skipping duplicate event eventId={}", event.eventId());
                return Result.duplicate();
            }

            Optional<ProcessedEvent> processed = pipeline.process(event);
            if (processed.isEmpty()) {
                log.info("Event filtered out eventId={} eventType={}", event.eventId(), event.eventType());
                return Result.filtered();
            }

            processedEventPublisher.publish(processed.get());
            return Result.processed(processed.get());
        } finally {
            processingMetrics.record(System.nanoTime() - startNanos);
        }
    }
}
