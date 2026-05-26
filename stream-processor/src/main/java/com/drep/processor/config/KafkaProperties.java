package com.drep.processor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "drep.kafka")
public class KafkaProperties {

    private Topics topics = new Topics();
    private Consumer rawConsumer = new Consumer();
    private Consumer processedConsumer = new Consumer();
    private Retry retry = new Retry();
    private LagMonitor lagMonitor = new LagMonitor();
    private Admin admin = new Admin();

    public Topics getTopics() {
        return topics;
    }

    public void setTopics(Topics topics) {
        this.topics = topics;
    }

    public Consumer getRawConsumer() {
        return rawConsumer;
    }

    public void setRawConsumer(Consumer rawConsumer) {
        this.rawConsumer = rawConsumer;
    }

    public Consumer getProcessedConsumer() {
        return processedConsumer;
    }

    public void setProcessedConsumer(Consumer processedConsumer) {
        this.processedConsumer = processedConsumer;
    }

    public Retry getRetry() {
        return retry;
    }

    public void setRetry(Retry retry) {
        this.retry = retry;
    }

    public LagMonitor getLagMonitor() {
        return lagMonitor;
    }

    public void setLagMonitor(LagMonitor lagMonitor) {
        this.lagMonitor = lagMonitor;
    }

    public Admin getAdmin() {
        return admin;
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
    }

    public static class Topics {
        private String raw = "events.raw";
        private String processed = "events.processed";
        private String dlq = "events.dlq";

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
    }

    public static class Consumer {
        private String groupId;

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }
    }

    public static class Retry {
        private int maxAttempts = 3;
        private long initialBackoffMs = 1000;
        private double backoffMultiplier = 2.0;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getInitialBackoffMs() {
            return initialBackoffMs;
        }

        public void setInitialBackoffMs(long initialBackoffMs) {
            this.initialBackoffMs = initialBackoffMs;
        }

        public double getBackoffMultiplier() {
            return backoffMultiplier;
        }

        public void setBackoffMultiplier(double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
        }
    }

    public static class LagMonitor {
        private long alertThreshold = 10_000;
        private long checkIntervalMs = 30_000;

        public long getAlertThreshold() {
            return alertThreshold;
        }

        public void setAlertThreshold(long alertThreshold) {
            this.alertThreshold = alertThreshold;
        }

        public long getCheckIntervalMs() {
            return checkIntervalMs;
        }

        public void setCheckIntervalMs(long checkIntervalMs) {
            this.checkIntervalMs = checkIntervalMs;
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
