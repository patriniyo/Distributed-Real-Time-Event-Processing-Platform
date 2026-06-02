package com.drep.analytics.scheduler;

import com.drep.analytics.aggregation.AggregationBucket;
import com.drep.analytics.aggregation.AggregationEngine;
import com.drep.analytics.service.AggregationPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class AggregationFlushScheduler {

    private static final Logger log = LoggerFactory.getLogger(AggregationFlushScheduler.class);

    private final AggregationEngine aggregationEngine;
    private final AggregationPersistenceService persistenceService;

    public AggregationFlushScheduler(AggregationEngine aggregationEngine,
                                       AggregationPersistenceService persistenceService) {
        this.aggregationEngine = aggregationEngine;
        this.persistenceService = persistenceService;
    }

    @Scheduled(fixedDelayString = "${drep.analytics.aggregation.flush-interval-ms:5000}")
    public void flushCompletedWindows() {
        Instant now = Instant.now();
        List<AggregationBucket> completed = aggregationEngine.flushCompletedBuckets(now);
        if (!completed.isEmpty()) {
            persistenceService.persistBuckets(completed);
            log.info("Flushed {} completed aggregation buckets", completed.size());
        }
    }
}
