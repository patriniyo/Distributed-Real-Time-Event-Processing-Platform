package com.drep.processor.service;

import com.drep.common.model.DlqEvent;
import com.drep.common.model.Event;
import com.drep.processor.config.KafkaProperties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

@Service
public class DlqInspectionService {

    private final org.springframework.boot.autoconfigure.kafka.KafkaProperties springKafkaProperties;
    private final KafkaProperties kafkaProperties;

    public DlqInspectionService(org.springframework.boot.autoconfigure.kafka.KafkaProperties springKafkaProperties,
                                  KafkaProperties kafkaProperties) {
        this.springKafkaProperties = springKafkaProperties;
        this.kafkaProperties = kafkaProperties;
    }

    public List<DlqEvent> inspect(int limit) {
        List<DlqEvent> results = new ArrayList<>();
        try (KafkaConsumer<String, DlqEvent> consumer = createDlqConsumer()) {
            consumer.subscribe(Collections.singletonList(kafkaProperties.getTopics().getDlq()));
            ConsumerRecords<String, DlqEvent> records = consumer.poll(Duration.ofSeconds(3));
            for (ConsumerRecord<String, DlqEvent> record : records) {
                results.add(record.value());
                if (results.size() >= limit) {
                    break;
                }
            }
        }
        return results;
    }

    public List<DlqEvent> inspectByDlqId(UUID dlqId) {
        return inspect(500).stream()
                .filter(event -> event.dlqId().equals(dlqId))
                .toList();
    }

    public Event replay(DlqEvent dlqEvent) {
        return dlqEvent.originalEvent();
    }

    private KafkaConsumer<String, DlqEvent> createDlqConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, springKafkaProperties.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "dlq-admin-inspector");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.drep.common.model");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, DlqEvent.class.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new KafkaConsumer<>(props);
    }
}
