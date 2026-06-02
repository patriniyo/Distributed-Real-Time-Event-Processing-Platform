package com.drep.dashboard.consumer;

import com.drep.common.model.ProcessedEvent;
import com.drep.dashboard.websocket.LiveEventWebSocketHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "drep.dashboard.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LiveFeedConsumer {

    private final LiveEventWebSocketHandler webSocketHandler;

    public LiveFeedConsumer(LiveEventWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @KafkaListener(
            topics = "${drep.dashboard.kafka.processed-topic:events.processed}",
            groupId = "${drep.dashboard.kafka.consumer-group:dashboard-live-feed-group}",
            containerFactory = "processedEventKafkaListenerContainerFactory"
    )
    public void consume(ProcessedEvent event, Acknowledgment acknowledgment) {
        webSocketHandler.broadcast(event);
        acknowledgment.acknowledge();
    }
}
