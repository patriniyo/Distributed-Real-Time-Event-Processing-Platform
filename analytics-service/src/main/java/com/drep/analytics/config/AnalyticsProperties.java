package com.drep.analytics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "drep.analytics")
public class AnalyticsProperties {

    private Kafka kafka = new Kafka();
    private Cache cache = new Cache();
    private Aggregation aggregation = new Aggregation();
    private Retention retention = new Retention();
    private Admin admin = new Admin();
    private Map<String, Integer> tenantRetentionDays = new HashMap<>();

    public Kafka getKafka() {
        return kafka;
    }

    public void setKafka(Kafka kafka) {
        this.kafka = kafka;
    }

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    public Aggregation getAggregation() {
        return aggregation;
    }

    public void setAggregation(Aggregation aggregation) {
        this.aggregation = aggregation;
    }

    public Retention getRetention() {
        return retention;
    }

    public void setRetention(Retention retention) {
        this.retention = retention;
    }

    public Admin getAdmin() {
        return admin;
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
    }

    public Map<String, Integer> getTenantRetentionDays() {
        return tenantRetentionDays;
    }

    public void setTenantRetentionDays(Map<String, Integer> tenantRetentionDays) {
        this.tenantRetentionDays = tenantRetentionDays;
    }

    public static class Kafka {
        private String processedTopic = "events.processed";
        private String consumerGroup = "analytics-service-group";

        public String getProcessedTopic() {
            return processedTopic;
        }

        public void setProcessedTopic(String processedTopic) {
            this.processedTopic = processedTopic;
        }

        public String getConsumerGroup() {
            return consumerGroup;
        }

        public void setConsumerGroup(String consumerGroup) {
            this.consumerGroup = consumerGroup;
        }
    }

    public static class Cache {
        private long ttlSeconds = 30;

        public long getTtlSeconds() {
            return ttlSeconds;
        }

        public void setTtlSeconds(long ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }
    }

    public static class Aggregation {
        private long flushIntervalMs = 5000;

        public long getFlushIntervalMs() {
            return flushIntervalMs;
        }

        public void setFlushIntervalMs(long flushIntervalMs) {
            this.flushIntervalMs = flushIntervalMs;
        }
    }

    public static class Retention {
        private int defaultDays = 90;
        private long cleanupIntervalMs = 86400000;

        public int getDefaultDays() {
            return defaultDays;
        }

        public void setDefaultDays(int defaultDays) {
            this.defaultDays = defaultDays;
        }

        public long getCleanupIntervalMs() {
            return cleanupIntervalMs;
        }

        public void setCleanupIntervalMs(long cleanupIntervalMs) {
            this.cleanupIntervalMs = cleanupIntervalMs;
        }
    }

    public static class Admin {
        private String apiKey = "admin-key";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }
}
