package com.drep.ingestion.service;

import com.drep.ingestion.config.IngestionProperties;
import com.drep.ingestion.exception.RateLimitExceededException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TenantRateLimitService {

    private final IngestionProperties properties;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public TenantRateLimitService(IngestionProperties properties) {
        this.properties = properties;
    }

    public void checkRateLimit(String tenantId, int requestWeight) {
        int limitRps = properties.getTenantRateLimits()
                .getOrDefault(tenantId, properties.getDefaultRateLimitRps());

        TokenBucket bucket = buckets.computeIfAbsent(tenantId, id -> new TokenBucket(limitRps));
        bucket.updateLimit(limitRps);

        if (!bucket.tryConsume(requestWeight)) {
            throw new RateLimitExceededException(tenantId, limitRps);
        }
    }

    public int getLimitForTenant(String tenantId) {
        return properties.getTenantRateLimits()
                .getOrDefault(tenantId, properties.getDefaultRateLimitRps());
    }

    /** Resets in-memory buckets (for tests). */
    public void reset() {
        buckets.clear();
    }

    /**
     * Simple token bucket: refills at {@code limitRps} tokens per second.
     */
    static final class TokenBucket {
        private volatile int limitRps;
        private final AtomicLong tokens;
        private volatile long lastRefillNanos;

        TokenBucket(int limitRps) {
            this.limitRps = limitRps;
            this.tokens = new AtomicLong(limitRps);
            this.lastRefillNanos = System.nanoTime();
        }

        void updateLimit(int newLimit) {
            this.limitRps = newLimit;
        }

        boolean tryConsume(int count) {
            refill();
            while (true) {
                long current = tokens.get();
                if (current < count) {
                    return false;
                }
                if (tokens.compareAndSet(current, current - count)) {
                    return true;
                }
            }
        }

        private void refill() {
            long now = System.nanoTime();
            long elapsed = now - lastRefillNanos;
            if (elapsed <= 0) {
                return;
            }
            long tokensToAdd = (elapsed * limitRps) / 1_000_000_000L;
            if (tokensToAdd > 0) {
                lastRefillNanos = now;
                tokens.updateAndGet(current -> Math.min(limitRps, current + tokensToAdd));
            }
        }
    }
}
