package com.drep.common.kafka;

public final class KafkaTopics {

    public static final String RAW = "events.raw";
    public static final String PROCESSED = "events.processed";
    public static final String DLQ = "events.dlq";

    private KafkaTopics() {
    }

    public static String partitionKey(String tenantId, String eventType) {
        return tenantId + ":" + eventType;
    }
}
