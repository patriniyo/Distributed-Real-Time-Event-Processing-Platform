# Distributed Real-Time Event Processing Platform

Java / Spring Boot monorepo implementing **EPIC 1 — Event Ingestion Layer** and **EPIC 2 — Message Broker & Queue Layer**.

### Project Structure

```
pom.xml                  # Maven parent (aggregator)
common-events/           # Shared Event + DlqEvent models
ingestion-service/       # Spring Boot REST API + API gateway + Kafka producer
stream-processor/        # Kafka consumer, DLQ, admin API
producer-sdk/            # Java client SDK for event producers
docker-compose.yml       # Local Kafka + Kafka UI
```

---

## Quick Start

### Build everything

```bash
mvn clean install
```

### Run Kafka (required for EPIC 2)

```bash
docker compose up -d
```

Kafka: `localhost:9092` · Kafka UI: `http://localhost:8085`

Topics created automatically: `events.raw`, `events.processed`, `events.dlq` (7-day retention, partitioned by `tenantId:eventType`).

### Run the ingestion service

```bash
cd ingestion-service
mvn spring-boot:run
```

The service starts on `http://localhost:8098` and publishes accepted events to `events.raw`.

### Run the stream processor

```bash
cd stream-processor
mvn spring-boot:run
```

Consumes `events.raw`, publishes to `events.processed`, routes failures to `events.dlq`. Runs on `http://localhost:8099`.

### DLQ admin API

```bash
# List DLQ events
curl -H "X-API-Key: admin-key" http://localhost:8099/admin/dlq

# Replay all (up to 100)
curl -X POST -H "X-API-Key: admin-key" http://localhost:8099/admin/dlq/replay

# Replay one event
curl -X POST -H "X-API-Key: admin-key" http://localhost:8099/admin/dlq/{dlqId}/replay
```

See `docs/kafka-consumer-groups.md` for consumer group and rebalancing details.

### Health check

```bash
curl http://localhost:8098/health
```

### Ingest a single event

```bash
curl -X POST http://localhost:8098/api/v1/events \
  -H "Content-Type: application/json" \
  -H "X-API-Key: tenant-a-key" \
  -d '{
    "eventType": "page.view",
    "timestamp": "2026-05-24T12:00:00Z",
    "tenantId": "tenant-a",
    "payload": {"page": "/home"}
  }'
```

Returns `202 Accepted` with `traceId` and assigned `eventIds`.

---

## Features Implemented

### F1.1 — REST Ingestion API

| Feature | Implementation |
|---------|----------------|
| `POST /api/v1/events` | Single event, `{ "events": [...] }` batch, or JSON array |
| Schema validation | `eventType`, `timestamp`, `tenantId`, `payload` (Bean Validation) |
| Async processing | `@Async` event processor, immediate `202 Accepted` |
| Per-tenant rate limiting | Token bucket, configurable RPS in `application.yml` |

### F1.2 — Producer SDK (Java)

| Feature | Implementation |
|---------|----------------|
| Retry | Exponential backoff on 5xx / network errors |
| Local buffer | In-memory queue with periodic flush |
| Async API | `publishAsync()` returns `CompletableFuture` |

### F1.3 — API Gateway

| Feature | Implementation |
|---------|----------------|
| API Key auth | `X-API-Key` header → tenant mapping |
| JWT auth | `Authorization: Bearer <token>` |
| Rate limiting | Per-tenant token bucket before ingestion |
| Request logging | Structured logs with `traceId`, method, path, status, duration |
| Trace ID injection | `X-Trace-Id` header (auto-generated if absent) |
| Health check | `GET /health` (public, no auth) |

---

## Configuration

Edit `ingestion-service/src/main/resources/application.yml`:

```yaml
ingestion:
  default-rate-limit-rps: 1000
  tenant-rate-limits:
    tenant-a: 500
  api-keys:
    tenant-a-key: tenant-a
  jwt:
    secret: ${JWT_SECRET:change-me-in-production...}
```

---

## Producer SDK Usage

Add the `producer-sdk` module as a dependency, then:

```java
try (EventProducer producer = EventProducer.builder("http://localhost:8098")
        .apiKey("tenant-a-key")
        .maxRetries(3)
        .build()) {

    EventRecord event = EventRecord.of("page.view", "tenant-a", Map.of("page", "/home"));
    producer.publishAsync(event).thenAccept(result ->
        System.out.println("Published: " + result.status()));
}
```

---

## Testing

```bash
mvn test
```

---

## Error Responses

All errors return structured JSON:

```json
{
  "error": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "traceId": "uuid",
  "timestamp": "2026-05-24T12:00:00Z",
  "fieldErrors": [
    { "field": "eventType", "message": "eventType is required" }
  ]
}
```
