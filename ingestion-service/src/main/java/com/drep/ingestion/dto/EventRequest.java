package com.drep.ingestion.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventRequest(
        @NotBlank(message = "eventType is required")
        String eventType,

        @NotNull(message = "timestamp is required")
        Instant timestamp,

        @NotBlank(message = "tenantId is required")
        String tenantId,

        @NotNull(message = "payload is required")
        JsonNode payload
) {
}
