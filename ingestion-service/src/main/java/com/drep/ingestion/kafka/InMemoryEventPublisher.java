package com.drep.ingestion.kafka;

import com.drep.common.model.Event;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.CopyOnWriteArrayList;

@Service
@ConditionalOnProperty(prefix = "drep.kafka", name = "enabled", havingValue = "false")
public class InMemoryEventPublisher implements EventPublisher {

    private final CopyOnWriteArrayList<Event> events = new CopyOnWriteArrayList<>();

    @Override
    public void publish(Event event) {
        events.add(event);
    }

    public java.util.List<Event> getPublishedEvents() {
        return java.util.List.copyOf(events);
    }

    public void clear() {
        events.clear();
    }
}
