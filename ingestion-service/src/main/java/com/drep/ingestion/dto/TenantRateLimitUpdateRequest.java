package com.drep.ingestion.dto;

import jakarta.validation.constraints.Min;

public record TenantRateLimitUpdateRequest(@Min(1) int rateLimitRps) {
}
