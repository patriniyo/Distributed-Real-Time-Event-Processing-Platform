package com.drep.ingestion.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.drep.ingestion.config.IngestionProperties;
import com.drep.ingestion.exception.RateLimitExceededException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantRateLimitServiceTest {

    private TenantRateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        IngestionProperties properties = new IngestionProperties();
        properties.setDefaultRateLimitRps(5);
        properties.setTenantRateLimits(Map.of("tenant-a", 3));
        rateLimitService = new TenantRateLimitService(properties);
    }

    @Test
    void allowsRequestsWithinLimit() {
        assertThatCode(() -> {
            for (int i = 0; i < 3; i++) {
                rateLimitService.checkRateLimit("tenant-a", 1);
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void rejectsRequestsOverLimit() {
        for (int i = 0; i < 3; i++) {
            rateLimitService.checkRateLimit("tenant-a", 1);
        }
        assertThatThrownBy(() -> rateLimitService.checkRateLimit("tenant-a", 1))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void usesDefaultLimitForUnknownTenant() {
        assertThatCode(() -> {
            for (int i = 0; i < 5; i++) {
                rateLimitService.checkRateLimit("unknown", 1);
            }
        }).doesNotThrowAnyException();
    }
}
