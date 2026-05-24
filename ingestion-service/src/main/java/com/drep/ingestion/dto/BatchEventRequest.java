package com.drep.ingestion.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BatchEventRequest(
        @NotEmpty(message = "events list must not be empty")
        @Size(max = 1000, message = "batch size must not exceed 1000 events")
        List<@Valid EventRequest> events
) {
}
