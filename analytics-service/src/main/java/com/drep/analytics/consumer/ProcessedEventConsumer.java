package com.drep.analytics.consumer;

import com.drep.analytics.aggregation.AggregationEngine;
import com.drep.analytics.service.RawEventStoreService;
import com.drep.common.model.ProcessedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class ProcessedEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProcessedEventConsumer.class);

    private final RawEventStoreService rawEventStoreService;
    private final AggregationEngine aggregationEngine;

    public ProcessedEventConsumer(RawEventStoreService rawEventStoreService,
                                  AggregationEngine aggregationEngine) {
        this.rawEventStoreService = rawEventStoreService;
        this.aggregationEngine = aggregationEngine;
    }

    @KafkaListener(
            topics = "${drep.analytics.kafka.processed-topic}",
            groupId = "${drep.analytics.kafka.consumer-group}",
            containerFactory = "processedEventKafkaListenerContainerFactory"
    )
    public void consume(ProcessedEvent event, Acknowledgment acknowledgment) {
        rawEventStoreService.persist(event);
        aggregationEngine.ingest(event);
        acknowledgment.acknowledge();
        log.debug("Ingested processed event eventId={} tenantId={} eventType={}",
                event.eventId(), event.tenantId(), event.eventType());
    }
}
