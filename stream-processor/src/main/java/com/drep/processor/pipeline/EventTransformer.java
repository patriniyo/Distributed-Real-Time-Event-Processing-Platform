package com.drep.processor.pipeline;

import com.drep.common.model.Event;

import java.util.Optional;

@FunctionalInterface
public interface EventTransformer {
    Optional<Event> transform(Event event);
}
