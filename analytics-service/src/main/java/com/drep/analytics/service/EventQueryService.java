package com.drep.analytics.service;

import com.drep.analytics.config.AnalyticsProperties;
import com.drep.analytics.domain.RawEventEntity;
import com.drep.analytics.dto.EventQueryResponse;
import com.drep.analytics.dto.EventSummaryDto;
import com.drep.analytics.repository.RawEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class EventQueryService {

    private final RawEventRepository rawEventRepository;

    public EventQueryService(RawEventRepository rawEventRepository) {
        this.rawEventRepository = rawEventRepository;
    }

    @Transactional(readOnly = true)
    public EventQueryResponse query(String tenantId, String eventType, Instant from, Instant to,
                                    int page, int size) {
        Instant rangeFrom = from != null ? from : Instant.now().minus(24, ChronoUnit.HOURS);
        Instant rangeTo = to != null ? to : Instant.now();

        Page<RawEventEntity> results = rawEventRepository.findEvents(
                tenantId, eventType, rangeFrom, rangeTo, PageRequest.of(page, size));

        return new EventQueryResponse(
                results.getContent().stream().map(this::toDto).toList(),
                results.getTotalElements(),
                page,
                size
        );
    }

    private EventSummaryDto toDto(RawEventEntity entity) {
        return new EventSummaryDto(
                entity.getEventId(),
                entity.getEventType(),
                entity.getTenantId(),
                entity.getEventTimestamp(),
                entity.getProcessedAt(),
                entity.getTraceId(),
                entity.getPayload(),
                entity.getEnrichment()
        );
    }
}
