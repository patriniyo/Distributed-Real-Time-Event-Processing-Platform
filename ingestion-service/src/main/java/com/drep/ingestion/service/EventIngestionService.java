package com.drep.ingestion.service;

import com.drep.common.model.Event;
import com.drep.ingestion.dto.EventRequest;
import com.drep.ingestion.kafka.EventPublisher;
import com.drep.ingestion.observability.IngestionMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EventIngestionService {

    private static final Logger log = LoggerFactory.getLogger(EventIngestionService.class);

    private final EventPublisher eventPublisher;
    private final IngestionMetrics ingestionMetrics;

    public EventIngestionService(EventPublisher eventPublisher, IngestionMetrics ingestionMetrics) {
        this.eventPublisher = eventPublisher;
        this.ingestionMetrics = ingestionMetrics;
    }

    public List<Event> acceptEvents(List<EventRequest> requests, String traceId) {
        List<Event> events = new ArrayList<>(requests.size());
        for (EventRequest request : requests) {
            Event event = Event.from(
                    request.eventType(),
                    request.timestamp(),
                    request.tenantId(),
                    request.payload(),
                    traceId
            );
            events.add(event);
            processAsync(event);
        }
        ingestionMetrics.recordIngested(events.size());
        return events;
    }

    @Async("eventProcessingExecutor")
    public void processAsync(Event event) {
        log.info("Processing event eventId={} eventType={} tenantId={} traceId={}",
                event.eventId(), event.eventType(), event.tenantId(), event.traceId());
        eventPublisher.publish(event);
    }
}
