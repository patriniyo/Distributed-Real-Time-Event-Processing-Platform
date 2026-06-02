package com.drep.analytics.dto;

import java.util.List;

public record EventQueryResponse(
        List<EventSummaryDto> events,
        long totalElements,
        int page,
        int size
) {
}
