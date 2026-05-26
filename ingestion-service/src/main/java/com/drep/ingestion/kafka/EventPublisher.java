package com.drep.ingestion.kafka;

import com.drep.common.model.Event;

public interface EventPublisher {
    void publish(Event event);
}
