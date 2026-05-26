package com.drep.processor.service;

import com.drep.common.kafka.KafkaTopics;
import com.drep.common.model.DlqEvent;
import com.drep.common.model.Event;
import com.drep.processor.config.KafkaProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DlqReplayService {

    private final DlqInspectionService dlqInspectionService;
    private final KafkaTemplate<String, Event> eventKafkaTemplate;
    private final KafkaProperties kafkaProperties;

    public DlqReplayService(DlqInspectionService dlqInspectionService,
                            KafkaTemplate<String, Event> eventKafkaTemplate,
                            KafkaProperties kafkaProperties) {
        this.dlqInspectionService = dlqInspectionService;
        this.eventKafkaTemplate = eventKafkaTemplate;
        this.kafkaProperties = kafkaProperties;
    }

    public int replayAll(int limit) {
        List<DlqEvent> dlqEvents = dlqInspectionService.inspect(limit);
        dlqEvents.forEach(this::replayOne);
        return dlqEvents.size();
    }

    public boolean replayByDlqId(UUID dlqId) {
        List<DlqEvent> matches = dlqInspectionService.inspectByDlqId(dlqId);
        if (matches.isEmpty()) {
            return false;
        }
        replayOne(matches.getFirst());
        return true;
    }

    private void replayOne(DlqEvent dlqEvent) {
        Event original = dlqEvent.originalEvent();
        String key = KafkaTopics.partitionKey(original.tenantId(), original.eventType());
        eventKafkaTemplate.send(kafkaProperties.getTopics().getRaw(), key, original);
    }
}
