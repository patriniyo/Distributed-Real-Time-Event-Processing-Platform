-- EPIC 7: tenants, scoped API keys, audit log
CREATE TABLE tenants (
    tenant_id    VARCHAR(255) PRIMARY KEY,
    status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE api_keys (
    id           UUID PRIMARY KEY,
    tenant_id    VARCHAR(255) NOT NULL REFERENCES tenants(tenant_id),
    key_hash     VARCHAR(64) NOT NULL UNIQUE,
    key_prefix   VARCHAR(16) NOT NULL,
    scope        VARCHAR(20) NOT NULL,
    role         VARCHAR(20) NOT NULL,
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    rotated_from UUID REFERENCES api_keys(id),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at   TIMESTAMPTZ
);

CREATE INDEX idx_api_keys_tenant ON api_keys (tenant_id) WHERE active = TRUE;
CREATE INDEX idx_api_keys_hash ON api_keys (key_hash) WHERE active = TRUE;

CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY,
    actor_key_id    UUID,
    actor_tenant_id VARCHAR(255),
    actor_role      VARCHAR(20),
    action          VARCHAR(100) NOT NULL,
    resource        VARCHAR(255) NOT NULL,
    details         TEXT,
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_occurred ON audit_logs (occurred_at DESC);
CREATE INDEX idx_audit_logs_tenant ON audit_logs (actor_tenant_id, occurred_at DESC);

INSERT INTO tenants (tenant_id, status) VALUES
    ('tenant-a', 'ACTIVE'),
    ('tenant-b', 'ACTIVE'),
    ('tenant-c', 'ACTIVE'),
    ('__platform__', 'ACTIVE');

-- Bootstrap keys (SHA-256 of plaintext): tenant-a-key, tenant-b-key, admin-key
INSERT INTO api_keys (id, tenant_id, key_hash, key_prefix, scope, role, active) VALUES
    ('b0000000-0000-0000-0000-000000000001', 'tenant-a', '0be0fb939640934a6916e052d172cc0068f0a265481d91f47358caae5bc26da3', 'tenant-a-k', 'INGEST', 'ENGINEER', TRUE),
    ('b0000000-0000-0000-0000-000000000002', 'tenant-b', 'aeadb7188c1bed04546ce2ce4522ea8d513c624ada0301e7d26d17532ec723b3', 'tenant-b-k', 'INGEST', 'ENGINEER', TRUE),
    ('b0000000-0000-0000-0000-000000000003', '__platform__', '69a5265506c94c77b787a7d7377b7685a0eff82e33920a71e7ee22cd6154953e', 'admin-key', 'ADMIN', 'ADMIN', TRUE);
