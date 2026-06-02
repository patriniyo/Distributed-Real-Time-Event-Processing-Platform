-- TimescaleDB aggregation metrics (EPIC 5 F5.2)
CREATE EXTENSION IF NOT EXISTS timescaledb;

CREATE TABLE aggregation_metrics (
    bucket_start  TIMESTAMPTZ NOT NULL,
    bucket_end    TIMESTAMPTZ NOT NULL,
    window_size   VARCHAR(10) NOT NULL,
    tenant_id     VARCHAR(255) NOT NULL,
    event_type    VARCHAR(255) NOT NULL,
    event_count   BIGINT NOT NULL DEFAULT 0,
    sum_value     DOUBLE PRECISION,
    avg_value     DOUBLE PRECISION,
    min_value     DOUBLE PRECISION,
    max_value     DOUBLE PRECISION,
    p50           DOUBLE PRECISION,
    p95           DOUBLE PRECISION,
    p99           DOUBLE PRECISION,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (tenant_id, event_type, window_size, bucket_start)
);

SELECT create_hypertable('aggregation_metrics', 'bucket_start', if_not_exists => TRUE);

CREATE INDEX idx_agg_metrics_tenant_window ON aggregation_metrics (tenant_id, window_size, bucket_start DESC);

-- Continuous aggregate: hourly rollups from 1-minute buckets
CREATE MATERIALIZED VIEW aggregation_metrics_hourly
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 hour', bucket_start) AS bucket_start,
    time_bucket('1 hour', bucket_start) + INTERVAL '1 hour' AS bucket_end,
    '1hr' AS window_size,
    tenant_id,
    event_type,
    SUM(event_count) AS event_count,
    SUM(sum_value) AS sum_value,
    AVG(avg_value) AS avg_value,
    MIN(min_value) AS min_value,
    MAX(max_value) AS max_value,
    AVG(p50) AS p50,
    AVG(p95) AS p95,
    AVG(p99) AS p99
FROM aggregation_metrics
WHERE window_size = '1m'
GROUP BY time_bucket('1 hour', bucket_start), tenant_id, event_type
WITH NO DATA;

SELECT add_continuous_aggregate_policy('aggregation_metrics_hourly',
    start_offset => INTERVAL '3 hours',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour');

-- Compression for data older than 30 days
ALTER TABLE aggregation_metrics SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'tenant_id, event_type, window_size'
);

SELECT add_compression_policy('aggregation_metrics', INTERVAL '30 days');
