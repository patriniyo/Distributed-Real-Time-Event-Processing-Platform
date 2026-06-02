package com.drep.analytics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AlertRuleRequest(
        @NotBlank String tenantId,
        String eventType,
        @NotBlank String metric,
        @NotBlank String operator,
        @NotNull Double threshold,
        String webhookUrl,
        boolean enabled
) {
    public boolean enabledOrDefault() {
        return enabled;
    }
}
