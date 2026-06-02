# Distributed Real-Time Event Processing Platform

Java / Spring Boot monorepo implementing event ingestion, Kafka streaming, analytics, storage, and a real-time dashboard (EPICs 1–6).

### Project Structure

```
pom.xml                  # Maven parent (aggregator)
common-events/           # Shared Event + DlqEvent models
ingestion-service/       # Spring Boot REST API + API gateway + Kafka producer
stream-processor/        # Kafka consumer, DLQ, admin API
analytics-service/       # Aggregations, storage, analytics API
dashboard-service/       # WebSocket live feed, REST BFF, React UI host
dashboard/               # React dashboard (Vite + Recharts)
producer-sdk/            # Java client SDK for event producers
docker-compose.yml       # Local Kafka + Redis + TimescaleDB + Kafka UI
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

Kafka: `localhost:9092` · TimescaleDB: `localhost:5432` · Redis: `6379` · Kafka UI: `http://localhost:8085`

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

### Event replay API (EPIC 3)

```bash
curl -X POST http://localhost:8099/admin/replay \
  -H "Content-Type: application/json" \
  -H "X-API-Key: admin-key" \
  -d '{
    "topic": "events.raw",
    "targetTopic": "events.raw",
    "from": "2026-05-24T00:00:00Z",
    "to": "2026-05-24T23:59:59Z",
    "tenantId": "tenant-a",
    "eventType": "page.view"
  }'

# Check replay job status
curl -H "X-API-Key: admin-key" http://localhost:8099/admin/replay/{jobId}
```

Replay runs in an isolated consumer group (`replay-{jobId}`) so live processing lag is unaffected.

See `docs/kafka-consumer-groups.md` for consumer group and rebalancing details.

### Run the analytics service

```bash
cd analytics-service
mvn spring-boot:run
```

Consumes `events.processed`, persists to PostgreSQL/TimescaleDB, computes aggregations, serves analytics API on `http://localhost:8100`.

### Analytics API

```bash
curl -H "X-API-Key: admin-key" \
  "http://localhost:8100/api/v1/analytics?tenant=tenant-a&eventType=page.view&window=1m"
```

Windows: `1m`, `5m`, `1hr`. Optional `groupBy=eventType` for grouped results. Cache TTL: 30s (Redis).

### Build and run the dashboard (EPIC 6)

Build the React UI into `dashboard-service` static assets (runs automatically on `mvn package` via frontend-maven-plugin, or manually):

```bash
cd dashboard
npm install
npm run build
```

This overwrites `dashboard-service/src/main/resources/static/` with the Vite + Recharts build.

Run the dashboard service (BFF + WebSocket + UI):

```bash
cd dashboard-service
mvn spring-boot:run
```

Open `http://localhost:8101` for the live dashboard. Swagger UI: `http://localhost:8101/swagger-ui.html`.

The dashboard service proxies REST calls to ingestion (`8098`), stream-processor (`8099`), and analytics (`8100`), and streams processed events over WebSocket:

```
WS /ws/events?tenant=tenant-a&eventType=page.view&token=admin-key
```

Unified REST BFF (all require `X-API-Key: admin-key`):

```bash
# Query analytics
curl -H "X-API-Key: admin-key" \
  "http://localhost:8101/api/v1/analytics?tenant=tenant-a&window=1m"

# Query historical events
curl -H "X-API-Key: admin-key" \
  "http://localhost:8101/api/v1/events?tenant=tenant-a"

# DLQ admin (proxied to stream-processor)
curl -H "X-API-Key: admin-key" http://localhost:8101/admin/dlq

# Tenant config (retention + rate limits)
curl -H "X-API-Key: admin-key" http://localhost:8101/admin/tenants
curl -X PUT -H "X-API-Key: admin-key" -H "Content-Type: application/json" \
  http://localhost:8101/admin/tenants/tenant-a \
  -d '{"retentionDays": 60, "rateLimitRps": 300}'
```

All services return errors using the shared `ApiErrorResponse` schema (`error`, `message`, `traceId`, `timestamp`, optional `fieldErrors`).

For local UI development with hot reload:

```bash
cd dashboard && npm run dev
```

Vite dev server proxies API/WebSocket calls to `http://localhost:8101`.

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
