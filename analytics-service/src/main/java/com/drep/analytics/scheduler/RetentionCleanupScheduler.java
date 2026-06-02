package com.drep.analytics.scheduler;

import com.drep.analytics.config.AnalyticsProperties;
import com.drep.analytics.repository.RawEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Component
public class RetentionCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(RetentionCleanupScheduler.class);

    private final RawEventRepository rawEventRepository;
    private final AnalyticsProperties properties;

    public RetentionCleanupScheduler(RawEventRepository rawEventRepository,
                                    AnalyticsProperties properties) {
        this.rawEventRepository = rawEventRepository;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${drep.analytics.retention.cleanup-interval-ms:86400000}")
    @Transactional
    public void cleanupExpiredEvents() {
        Map<String, Integer> tenantRetention = properties.getTenantRetentionDays();
        int defaultDays = properties.getRetention().getDefaultDays();

        if (tenantRetention.isEmpty()) {
            tenantRetention = Map.of("tenant-a", defaultDays, "tenant-b", defaultDays);
        }

        for (Map.Entry<String, Integer> entry : tenantRetention.entrySet()) {
            Instant cutoff = Instant.now().minus(entry.getValue(), ChronoUnit.DAYS);
            int deleted = rawEventRepository.deleteOlderThan(entry.getKey(), cutoff);
            if (deleted > 0) {
                log.info("Retention cleanup tenantId={} deleted={} olderThan={}",
                        entry.getKey(), deleted, cutoff);
            }
        }
    }
}
