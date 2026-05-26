package com.drep.processor.service;

import com.drep.common.model.Event;
import com.drep.processor.config.ProcessorProperties;
import org.springframework.stereotype.Service;

@Service
public class EventTransformationService {

    private final ProcessorProperties processorProperties;

    public EventTransformationService(ProcessorProperties processorProperties) {
        this.processorProperties = processorProperties;
    }

    public Event transform(Event event) {
        if (processorProperties.getFailEventTypes().contains(event.eventType())) {
            throw new IllegalStateException("Forced processing failure for eventType=" + event.eventType());
        }
        return event;
    }
}
