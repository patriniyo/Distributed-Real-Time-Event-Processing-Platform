-- Raw event store (EPIC 5 F5.1) — partitioned by event date
CREATE TABLE raw_events (
    event_id       UUID PRIMARY KEY,
    event_type     VARCHAR(255) NOT NULL,
    tenant_id      VARCHAR(255) NOT NULL,
    event_timestamp TIMESTAMPTZ NOT NULL,
    ingested_at    TIMESTAMPTZ NOT NULL,
    processed_at   TIMESTAMPTZ NOT NULL,
    trace_id       VARCHAR(255),
    schema_version INT NOT NULL DEFAULT 1,
    payload        JSONB,
    enrichment     JSONB,
    event_date     DATE NOT NULL GENERATED ALWAYS AS ((event_timestamp AT TIME ZONE 'UTC')::date) STORED
);

CREATE INDEX idx_raw_events_tenant_date ON raw_events (tenant_id, event_date);
CREATE INDEX idx_raw_events_tenant_type_ts ON raw_events (tenant_id, event_type, event_timestamp);
CREATE INDEX idx_raw_events_event_type ON raw_events (event_type);

COMMENT ON TABLE raw_events IS 'Processed events persisted for historical query; event_date supports date partitioning strategy';
