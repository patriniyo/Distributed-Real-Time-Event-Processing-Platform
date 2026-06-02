package com.drep.processor.service;

import com.drep.common.kafka.KafkaTopics;
import com.drep.common.model.ProcessedEvent;
import com.drep.processor.config.KafkaProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProcessedEventPublisher {

    private final KafkaTemplate<String, ProcessedEvent> processedEventKafkaTemplate;
    private final KafkaProperties kafkaProperties;

    public ProcessedEventPublisher(KafkaTemplate<String, ProcessedEvent> processedEventKafkaTemplate,
                                   KafkaProperties kafkaProperties) {
        this.processedEventKafkaTemplate = processedEventKafkaTemplate;
        this.kafkaProperties = kafkaProperties;
    }

    public void publish(ProcessedEvent event) {
        String key = KafkaTopics.partitionKey(event.tenantId(), event.eventType());
        processedEventKafkaTemplate.send(kafkaProperties.getTopics().getProcessed(), key, event);
        processedEventKafkaTemplate.flush();
    }
}
