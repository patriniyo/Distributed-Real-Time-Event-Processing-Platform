package com.drep.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "drep.kafka")
public class KafkaProperties {

    private boolean enabled = true;
    private Topics topics = new Topics();
    private Producer producer = new Producer();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Topics getTopics() {
        return topics;
    }

    public void setTopics(Topics topics) {
        this.topics = topics;
    }

    public Producer getProducer() {
        return producer;
    }

    public void setProducer(Producer producer) {
        this.producer = producer;
    }

    public static class Topics {
        private String raw = "events.raw";
        private String processed = "events.processed";
        private String dlq = "events.dlq";
        private int partitions = 12;
        private long retentionMs = 604_800_000L;
        private short replicationFactor = 1;

        public String getRaw() {
            return raw;
        }

        public void setRaw(String raw) {
            this.raw = raw;
        }

        public String getProcessed() {
            return processed;
        }

        public void setProcessed(String processed) {
            this.processed = processed;
        }

        public String getDlq() {
            return dlq;
        }

        public void setDlq(String dlq) {
            this.dlq = dlq;
        }

        public int getPartitions() {
            return partitions;
        }

        public void setPartitions(int partitions) {
            this.partitions = partitions;
        }

        public long getRetentionMs() {
            return retentionMs;
        }

        public void setRetentionMs(long retentionMs) {
            this.retentionMs = retentionMs;
        }

        public short getReplicationFactor() {
            return replicationFactor;
        }

        public void setReplicationFactor(short replicationFactor) {
            this.replicationFactor = replicationFactor;
        }
    }

    public static class Producer {
        private String acks = "all";
        private int retries = 3;

        public String getAcks() {
            return acks;
        }

        public void setAcks(String acks) {
            this.acks = acks;
        }

        public int getRetries() {
            return retries;
        }

        public void setRetries(int retries) {
            this.retries = retries;
        }
    }
}
