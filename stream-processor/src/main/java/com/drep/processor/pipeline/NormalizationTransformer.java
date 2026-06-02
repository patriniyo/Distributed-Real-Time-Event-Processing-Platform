package com.drep.processor.pipeline;

import com.drep.common.model.Event;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class NormalizationTransformer implements EventTransformer {

    @Override
    public Optional<Event> transform(Event event) {
        String normalizedType = event.eventType() == null ? "" : event.eventType().trim().toLowerCase();
        if (normalizedType.isBlank()) {
            return Optional.empty();
        }
        if (normalizedType.equals(event.eventType())) {
            return Optional.of(event);
        }
        return Optional.of(new Event(
                event.eventId(),
                normalizedType,
                event.timestamp(),
                event.tenantId(),
                event.payload(),
                event.traceId(),
                event.ingestedAt()
        ));
    }
}
