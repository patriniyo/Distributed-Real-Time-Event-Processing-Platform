package com.drep.processor.service;

import com.drep.common.kafka.KafkaTopics;
import com.drep.common.model.Event;
import com.drep.processor.config.KafkaProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProcessedEventPublisher {

    private final KafkaTemplate<String, Event> eventKafkaTemplate;
    private final KafkaProperties kafkaProperties;

    public ProcessedEventPublisher(KafkaTemplate<String, Event> eventKafkaTemplate,
                                   KafkaProperties kafkaProperties) {
        this.eventKafkaTemplate = eventKafkaTemplate;
        this.kafkaProperties = kafkaProperties;
    }

    public void publish(Event event) {
        String key = KafkaTopics.partitionKey(event.tenantId(), event.eventType());
        eventKafkaTemplate.send(kafkaProperties.getTopics().getProcessed(), key, event);
    }
}
