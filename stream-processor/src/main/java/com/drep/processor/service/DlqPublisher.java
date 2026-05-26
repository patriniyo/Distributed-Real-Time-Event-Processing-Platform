package com.drep.processor.service;

import com.drep.common.kafka.KafkaTopics;
import com.drep.common.model.DlqEvent;
import com.drep.common.model.Event;
import com.drep.processor.config.KafkaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class DlqPublisher {

    private static final Logger log = LoggerFactory.getLogger(DlqPublisher.class);

    private final KafkaTemplate<String, DlqEvent> dlqKafkaTemplate;
    private final KafkaProperties kafkaProperties;

    public DlqPublisher(KafkaTemplate<String, DlqEvent> dlqKafkaTemplate, KafkaProperties kafkaProperties) {
        this.dlqKafkaTemplate = dlqKafkaTemplate;
        this.kafkaProperties = kafkaProperties;
    }

    public DlqEvent publish(Event event, String failureReason, int retryCount) {
        DlqEvent dlqEvent = DlqEvent.of(
                event,
                failureReason,
                retryCount,
                kafkaProperties.getTopics().getRaw(),
                kafkaProperties.getRawConsumer().getGroupId()
        );
        String key = KafkaTopics.partitionKey(event.tenantId(), event.eventType());
        dlqKafkaTemplate.send(kafkaProperties.getTopics().getDlq(), key, dlqEvent)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish DLQ event dlqId={}", dlqEvent.dlqId(), ex);
                    }
                });
        dlqKafkaTemplate.flush();
        log.warn("Routed event to DLQ dlqId={} eventId={} retryCount={} reason={}",
                dlqEvent.dlqId(), event.eventId(), retryCount, failureReason);
        return dlqEvent;
    }
}
