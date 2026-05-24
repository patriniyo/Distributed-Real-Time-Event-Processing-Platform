package com.drep.ingestion.service;

import com.drep.ingestion.dto.EventRequest;
import com.drep.ingestion.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class EventIngestionService {

    private static final Logger log = LoggerFactory.getLogger(EventIngestionService.class);

    /** In-memory buffer for downstream integration (Kafka in Epic 2). */
    private final CopyOnWriteArrayList<Event> processedEvents = new CopyOnWriteArrayList<>();

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
        return events;
    }

    @Async("eventProcessingExecutor")
    public void processAsync(Event event) {
        log.info("Processing event eventId={} eventType={} tenantId={} traceId={}",
                event.eventId(), event.eventType(), event.tenantId(), event.traceId());
        processedEvents.add(event);
    }

    public List<Event> getProcessedEvents() {
        return List.copyOf(processedEvents);
    }

    public void clearProcessedEvents() {
        processedEvents.clear();
    }
}
