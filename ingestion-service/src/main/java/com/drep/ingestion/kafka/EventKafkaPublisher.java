package com.drep.ingestion.kafka;

import com.drep.common.kafka.KafkaTopics;
import com.drep.common.model.Event;
import com.drep.ingestion.config.KafkaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "drep.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EventKafkaPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventKafkaPublisher.class);

    private final KafkaTemplate<String, Event> kafkaTemplate;
    private final KafkaProperties kafkaProperties;

    public EventKafkaPublisher(KafkaTemplate<String, Event> kafkaTemplate, KafkaProperties kafkaProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaProperties = kafkaProperties;
    }

    @Override
    public void publish(Event event) {
        String key = KafkaTopics.partitionKey(event.tenantId(), event.eventType());
        String topic = kafkaProperties.getTopics().getRaw();

        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event to Kafka eventId={} topic={} key={}",
                                event.eventId(), topic, key, ex);
                    } else {
                        log.info("Published event to Kafka eventId={} topic={} partition={} offset={}",
                                event.eventId(),
                                topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
