package com.drep.processor.monitor;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ConsumerLagMonitor {

    private static final Logger log = LoggerFactory.getLogger(ConsumerLagMonitor.class);

    private final AdminClient adminClient;
    private final com.drep.processor.config.KafkaProperties drepKafkaProperties;
    private final AtomicLong lastReportedLag = new AtomicLong(0);
    private volatile boolean alertActive;

    public ConsumerLagMonitor(KafkaProperties springKafkaProperties,
                              com.drep.processor.config.KafkaProperties drepKafkaProperties) {
        this.adminClient = AdminClient.create(springKafkaProperties.buildAdminProperties(null));
        this.drepKafkaProperties = drepKafkaProperties;
    }

    @Scheduled(fixedDelayString = "${drep.kafka.lag-monitor.check-interval-ms:30000}")
    public void checkLag() {
        String groupId = drepKafkaProperties.getRawConsumer().getGroupId();
        String topic = drepKafkaProperties.getTopics().getRaw();
        try {
            long lag = computeLag(groupId, topic);
            lastReportedLag.set(lag);
            long threshold = drepKafkaProperties.getLagMonitor().getAlertThreshold();

            if (lag > threshold) {
                if (!alertActive) {
                    log.error("ALERT: consumer lag {} exceeds threshold {} for group={} topic={}",
                            lag, threshold, groupId, topic);
                    alertActive = true;
                }
            } else if (alertActive) {
                log.info("Consumer lag recovered to {} for group={} topic={}", lag, groupId, topic);
                alertActive = false;
            } else {
                log.debug("Consumer lag={} group={} topic={}", lag, groupId, topic);
            }
        } catch (Exception e) {
            log.warn("Failed to compute consumer lag for group={}: {}", groupId, e.getMessage());
        }
    }

    public long getLastReportedLag() {
        return lastReportedLag.get();
    }

    public boolean isAlertActive() {
        return alertActive;
    }

    private long computeLag(String groupId, String topic)
            throws ExecutionException, InterruptedException {
        ListConsumerGroupOffsetsResult offsetsResult = adminClient.listConsumerGroupOffsets(groupId);
        Map<TopicPartition, OffsetAndMetadata> committed =
                offsetsResult.partitionsToOffsetAndMetadata().get();

        Map<TopicPartition, OffsetSpec> request = new HashMap<>();
        committed.keySet().stream()
                .filter(tp -> tp.topic().equals(topic))
                .forEach(tp -> request.put(tp, OffsetSpec.latest()));

        if (request.isEmpty()) {
            return 0;
        }

        ListOffsetsResult latestOffsets = adminClient.listOffsets(request);
        long totalLag = 0;
        for (TopicPartition tp : request.keySet()) {
            long endOffset = latestOffsets.partitionResult(tp).get().offset();
            OffsetAndMetadata committedOffset = committed.get(tp);
            long current = committedOffset != null ? committedOffset.offset() : 0;
            totalLag += Math.max(0, endOffset - current);
        }
        return totalLag;
    }
}
