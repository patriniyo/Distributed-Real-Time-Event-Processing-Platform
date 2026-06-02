package com.drep.ingestion.dto;

public record TenantRateLimitDto(String tenantId, int rateLimitRps) {
}
