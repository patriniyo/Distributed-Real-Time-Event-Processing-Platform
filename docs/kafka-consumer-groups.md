# Kafka Consumer Groups & Rebalancing (EPIC 2 — F2.2)

## Consumer groups in this platform

| Service | Group ID | Topic | Purpose |
|---------|----------|-------|---------|
| stream-processor | `stream-processor-group` | `events.raw` | Transform and publish to `events.processed` |
| analytics (future) | `processed-analytics-group` | `events.processed` | Aggregations (EPIC 4) |
| DLQ admin inspector | `dlq-admin-inspector` | `events.dlq` | Read-only DLQ inspection (not a processing group) |

Each processing service uses a **dedicated consumer group** so offsets are independent and services scale horizontally without interfering with each other.

## Offset management

- **Manual commits** (`ENABLE_AUTO_COMMIT=false`, `AckMode.MANUAL`)
- Offsets are committed **only after successful processing** in `RawEventConsumer`
- Failed events after max retries are routed to `events.dlq`, then the offset is committed to avoid poison-pill loops

## Partition assignment strategy

Spring Kafka uses the Kafka consumer default: **`RangeAssignor`** (with **`CooperativeStickyAssignor`** available in modern clients).

When consumers join or leave the group:

1. The group coordinator triggers a **rebalance**
2. Partitions are redistributed across active consumers in the same group
3. Processing pauses briefly during rebalance (stop-the-world with classic assignors)

### Recommended production settings

For EPIC 3+ horizontal scaling, configure cooperative rebalancing to reduce downtime:

```yaml
spring:
  kafka:
    consumer:
      properties:
        partition.assignment.strategy: org.apache.kafka.clients.consumer.CooperativeStickyAssignor
```

## Partition key

Messages use key `tenantId:eventType` so events for the same tenant + type land in the same partition, preserving order per key.

## Durability (no message loss under broker failure)

- Producers use `acks=all` and `retries=3`
- Production replication factor **≥ 3** (`application-prod.yml`, `KAFKA_REPLICATION_FACTOR=3`)
- Topics: `events.raw`, `events.processed`, `events.dlq`
- Default retention: **7 days** (`retention.ms=604800000`)

## Consumer lag alerting

`ConsumerLagMonitor` in stream-processor checks lag every 30s and logs **ERROR** when lag exceeds **10,000** messages.
