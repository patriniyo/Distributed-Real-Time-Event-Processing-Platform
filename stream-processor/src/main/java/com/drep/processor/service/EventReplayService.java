package com.drep.processor.service;

import com.drep.common.kafka.KafkaTopics;
import com.drep.common.model.Event;
import com.drep.processor.config.ProcessorProperties;
import com.drep.processor.dto.ReplayJobResponse;
import com.drep.processor.dto.ReplayRequest;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Service
public class EventReplayService {

    private static final Logger log = LoggerFactory.getLogger(EventReplayService.class);

    private final KafkaProperties springKafkaProperties;
    private final KafkaTemplate<String, Event> eventKafkaTemplate;
    private final ProcessorProperties processorProperties;
    private final Executor replayExecutor;
    private final Map<UUID, ReplayJobResponse> jobs = new ConcurrentHashMap<>();

    public EventReplayService(KafkaProperties springKafkaProperties,
                              KafkaTemplate<String, Event> eventKafkaTemplate,
                              ProcessorProperties processorProperties,
                              @Qualifier("replayExecutor") Executor replayExecutor) {
        this.springKafkaProperties = springKafkaProperties;
        this.eventKafkaTemplate = eventKafkaTemplate;
        this.processorProperties = processorProperties;
        this.replayExecutor = replayExecutor;
    }

    public ReplayJobResponse startReplay(ReplayRequest request) {
        UUID jobId = UUID.randomUUID();
        String consumerGroup = processorProperties.getReplay().getGroupIdPrefix() + "-" + jobId;
        ReplayJobResponse running = ReplayJobResponse.running(jobId, consumerGroup);
        jobs.put(jobId, running);

        replayExecutor.execute(() -> {
            try {
                int replayed = replay(request, consumerGroup);
                jobs.put(jobId, ReplayJobResponse.completed(jobId, consumerGroup, replayed));
                log.info("Replay job {} completed replayed={} group={}", jobId, replayed, consumerGroup);
            } catch (Exception e) {
                jobs.put(jobId, ReplayJobResponse.failed(jobId, consumerGroup, e.getMessage()));
                log.error("Replay job {} failed", jobId, e);
            }
        });

        return running;
    }

    public ReplayJobResponse getJob(UUID jobId) {
        return jobs.get(jobId);
    }

    int replay(ReplayRequest request, String consumerGroup) {
        int maxRecords = processorProperties.getReplay().getMaxRecords();
        int replayed = 0;

        try (KafkaConsumer<String, Event> consumer = createReplayConsumer(consumerGroup)) {
            var partitionInfos = consumer.partitionsFor(request.topic());
            if (partitionInfos == null || partitionInfos.isEmpty()) {
                return 0;
            }

            List<TopicPartition> partitions = partitionInfos.stream()
                    .map(info -> new TopicPartition(request.topic(), info.partition()))
                    .toList();

            consumer.assign(partitions);
            Map<TopicPartition, Long> timestampQuery = new HashMap<>();
            partitions.forEach(tp -> timestampQuery.put(tp, request.from().toEpochMilli()));
            Map<TopicPartition, OffsetAndTimestamp> offsetsForTimes = consumer.offsetsForTimes(timestampQuery);

            for (TopicPartition partition : partitions) {
                OffsetAndTimestamp offsetAndTimestamp = offsetsForTimes.get(partition);
                if (offsetAndTimestamp != null) {
                    consumer.seek(partition, offsetAndTimestamp.offset());
                } else {
                    consumer.seekToEnd(Collections.singletonList(partition));
                }
            }

            boolean done = false;
            while (!done && replayed < maxRecords) {
                ConsumerRecords<String, Event> records = consumer.poll(Duration.ofMillis(500));
                if (records.isEmpty()) {
                    break;
                }
                for (ConsumerRecord<String, Event> record : records) {
                    if (record.timestamp() > request.to().toEpochMilli()) {
                        done = true;
                        break;
                    }
                    Event event = record.value();
                    if (!matchesFilters(event, request)) {
                        continue;
                    }
                    String key = KafkaTopics.partitionKey(event.tenantId(), event.eventType());
                    eventKafkaTemplate.send(request.resolvedTargetTopic(), key, event);
                    replayed++;
                    if (replayed >= maxRecords) {
                        done = true;
                        break;
                    }
                }
            }
            eventKafkaTemplate.flush();
        }
        return replayed;
    }

    private boolean matchesFilters(Event event, ReplayRequest request) {
        if (request.tenantId() != null && !request.tenantId().equals(event.tenantId())) {
            return false;
        }
        if (request.eventType() != null && !request.eventType().equals(event.eventType())) {
            return false;
        }
        return true;
    }

    private KafkaConsumer<String, Event> createReplayConsumer(String groupId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, springKafkaProperties.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.drep.common.model");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Event.class.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new KafkaConsumer<>(props);
    }
}
