package com.drep.analytics.dto;

import jakarta.validation.constraints.Min;

public record TenantRetentionUpdateRequest(@Min(1) int retentionDays) {
}
