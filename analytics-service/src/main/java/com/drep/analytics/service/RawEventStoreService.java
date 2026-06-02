package com.drep.analytics.service;

import com.drep.analytics.domain.RawEventEntity;
import com.drep.analytics.repository.RawEventRepository;
import com.drep.common.model.ProcessedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RawEventStoreService {

    private static final Logger log = LoggerFactory.getLogger(RawEventStoreService.class);

    private final RawEventRepository rawEventRepository;
    private final ObjectMapper objectMapper;

    public RawEventStoreService(RawEventRepository rawEventRepository, ObjectMapper objectMapper) {
        this.rawEventRepository = rawEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void persist(ProcessedEvent event) {
        if (rawEventRepository.existsById(event.eventId())) {
            return;
        }
        RawEventEntity entity = new RawEventEntity();
        entity.setEventId(event.eventId());
        entity.setEventType(event.eventType());
        entity.setTenantId(event.tenantId());
        entity.setEventTimestamp(event.timestamp());
        entity.setIngestedAt(event.ingestedAt());
        entity.setProcessedAt(event.processedAt());
        entity.setTraceId(event.traceId());
        entity.setSchemaVersion(event.schemaVersion());
        entity.setPayload(toJson(event.payload()));
        entity.setEnrichment(toJson(event.enrichment()));
        rawEventRepository.save(entity);
        log.debug("Persisted raw event eventId={} tenantId={}", event.eventId(), event.tenantId());
    }

    private String toJson(Object node) {
        if (node == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
