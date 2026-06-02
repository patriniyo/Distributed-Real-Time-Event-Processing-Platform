package com.drep.processor.pipeline;

import com.drep.common.model.Event;
import com.drep.processor.config.ProcessorProperties;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class FilterTransformer implements EventTransformer {

    private final ProcessorProperties processorProperties;

    public FilterTransformer(ProcessorProperties processorProperties) {
        this.processorProperties = processorProperties;
    }

    @Override
    public Optional<Event> transform(Event event) {
        if (processorProperties.getBlockedEventTypes().contains(event.eventType())) {
            return Optional.empty();
        }
        return Optional.of(event);
    }
}
