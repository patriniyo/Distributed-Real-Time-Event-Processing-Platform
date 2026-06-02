-- Alert rules (EPIC 4 F4.3)
CREATE TABLE alert_rules (
    id           UUID PRIMARY KEY,
    tenant_id    VARCHAR(255) NOT NULL,
    event_type   VARCHAR(255),
    metric       VARCHAR(50) NOT NULL,
    operator     VARCHAR(10) NOT NULL,
    threshold    DOUBLE PRECISION NOT NULL,
    webhook_url  VARCHAR(2048),
    enabled      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE alert_history (
    id           UUID PRIMARY KEY,
    rule_id      UUID NOT NULL REFERENCES alert_rules(id),
    tenant_id    VARCHAR(255) NOT NULL,
    event_type   VARCHAR(255),
    metric       VARCHAR(50) NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    threshold    DOUBLE PRECISION NOT NULL,
    fired_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    webhook_status VARCHAR(50),
    message      TEXT
);

CREATE INDEX idx_alert_rules_tenant ON alert_rules (tenant_id) WHERE enabled = TRUE;

-- Tenant retention config (EPIC 5 F5.1)
CREATE TABLE tenant_retention_config (
    tenant_id        VARCHAR(255) PRIMARY KEY,
    retention_days   INT NOT NULL DEFAULT 90
);

INSERT INTO tenant_retention_config (tenant_id, retention_days) VALUES
    ('tenant-a', 90),
    ('tenant-b', 90);

-- Demo alert rule
INSERT INTO alert_rules (id, tenant_id, event_type, metric, operator, threshold, webhook_url, enabled)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'tenant-a',
    'order.placed',
    'event_count',
    'gt',
    100,
    'http://localhost:8100/internal/alerts/webhook-test',
    FALSE
);
