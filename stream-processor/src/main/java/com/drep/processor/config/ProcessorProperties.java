package com.drep.processor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "drep.processor")
public class ProcessorProperties {

    private int schemaVersion = 1;
    private List<String> blockedEventTypes = new ArrayList<>();
    private List<String> failEventTypes = new ArrayList<>();
    private Map<String, String> tenantTiers = new HashMap<>();
    private Idempotency idempotency = new Idempotency();
    private Replay replay = new Replay();

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public List<String> getBlockedEventTypes() {
        return blockedEventTypes;
    }

    public void setBlockedEventTypes(List<String> blockedEventTypes) {
        this.blockedEventTypes = blockedEventTypes;
    }

    public List<String> getFailEventTypes() {
        return failEventTypes;
    }

    public void setFailEventTypes(List<String> failEventTypes) {
        this.failEventTypes = failEventTypes;
    }

    public Map<String, String> getTenantTiers() {
        return tenantTiers;
    }

    public void setTenantTiers(Map<String, String> tenantTiers) {
        this.tenantTiers = tenantTiers;
    }

    public Idempotency getIdempotency() {
        return idempotency;
    }

    public void setIdempotency(Idempotency idempotency) {
        this.idempotency = idempotency;
    }

    public Replay getReplay() {
        return replay;
    }

    public void setReplay(Replay replay) {
        this.replay = replay;
    }

    public static class Idempotency {
        private boolean enabled = true;
        private long ttlHours = 24;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getTtlHours() {
            return ttlHours;
        }

        public void setTtlHours(long ttlHours) {
            this.ttlHours = ttlHours;
        }
    }

    public static class Replay {
        private String groupIdPrefix = "replay";
        private int maxRecords = 10_000;

        public String getGroupIdPrefix() {
            return groupIdPrefix;
        }

        public void setGroupIdPrefix(String groupIdPrefix) {
            this.groupIdPrefix = groupIdPrefix;
        }

        public int getMaxRecords() {
            return maxRecords;
        }

        public void setMaxRecords(int maxRecords) {
            this.maxRecords = maxRecords;
        }
    }
}
