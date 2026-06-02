package com.drep.processor.consumer;

import com.drep.common.model.Event;
import com.drep.processor.config.KafkaProperties;
import com.drep.processor.service.DlqPublisher;
import com.drep.processor.service.EventProcessingService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class RawEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(RawEventConsumer.class);

    private final EventProcessingService eventProcessingService;
    private final DlqPublisher dlqPublisher;
    private final KafkaProperties kafkaProperties;

    public RawEventConsumer(EventProcessingService eventProcessingService,
                            DlqPublisher dlqPublisher,
                            KafkaProperties kafkaProperties) {
        this.eventProcessingService = eventProcessingService;
        this.dlqPublisher = dlqPublisher;
        this.kafkaProperties = kafkaProperties;
    }

    @KafkaListener(
            topics = "${drep.kafka.topics.raw}",
            groupId = "${drep.kafka.raw-consumer.group-id}",
            containerFactory = "rawEventKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, Event> record, Acknowledgment acknowledgment) {
        Event event = record.value();
        int maxAttempts = kafkaProperties.getRetry().getMaxAttempts();
        long backoffMs = kafkaProperties.getRetry().getInitialBackoffMs();
        Exception lastError = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                EventProcessingService.Result result = eventProcessingService.process(event);
                acknowledgment.acknowledge();
                log.info("Handled event eventId={} outcome={} partition={} offset={} attempt={}",
                        event.eventId(), result.outcome(), record.partition(), record.offset(), attempt);
                return;
            } catch (Exception ex) {
                lastError = ex;
                log.warn("Processing failed eventId={} attempt={}/{}: {}",
                        event.eventId(), attempt, maxAttempts, ex.getMessage());
                if (attempt < maxAttempts) {
                    sleep(backoffMs);
                    backoffMs = (long) (backoffMs * kafkaProperties.getRetry().getBackoffMultiplier());
                }
            }
        }

        String reason = lastError != null ? lastError.getMessage() : "Unknown processing failure";
        dlqPublisher.publish(event, reason, maxAttempts);
        acknowledgment.acknowledge();
    }

    private void sleep(long backoffMs) {
        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
