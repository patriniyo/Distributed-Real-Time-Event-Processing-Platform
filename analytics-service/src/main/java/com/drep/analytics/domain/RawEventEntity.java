package com.drep.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "raw_events")
public class RawEventEntity {

    @Id
    private UUID eventId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String tenantId;

    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    @Column(nullable = false)
    private Instant ingestedAt;

    @Column(nullable = false)
    private Instant processedAt;

    private String traceId;

    private int schemaVersion;

    @Column(columnDefinition = "jsonb")
    private String payload;

    @Column(columnDefinition = "jsonb")
    private String enrichment;

    @Column(name = "event_date", insertable = false, updatable = false)
    private LocalDate eventDate;

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(Instant eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public Instant getIngestedAt() {
        return ingestedAt;
    }

    public void setIngestedAt(Instant ingestedAt) {
        this.ingestedAt = ingestedAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getEnrichment() {
        return enrichment;
    }

    public void setEnrichment(String enrichment) {
        this.enrichment = enrichment;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }
}
