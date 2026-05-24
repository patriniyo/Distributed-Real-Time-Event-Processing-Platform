package com.drep.sdk;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EventProducer implements AutoCloseable {

    private static final Logger log = Logger.getLogger(EventProducer.class.getName());

    private final String baseUrl;
    private final String apiKey;
    private final String jwtToken;
    private final int maxRetries;
    private final Duration initialBackoff;
    private final double backoffMultiplier;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ConcurrentLinkedQueue<EventRecord> buffer = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService flushScheduler;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private EventProducer(Builder builder) {
        this.baseUrl = builder.baseUrl.replaceAll("/$", "");
        this.apiKey = builder.apiKey;
        this.jwtToken = builder.jwtToken;
        this.maxRetries = builder.maxRetries;
        this.initialBackoff = builder.initialBackoff;
        this.backoffMultiplier = builder.backoffMultiplier;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(builder.connectTimeout)
                .build();
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.flushScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "drep-sdk-flush");
            t.setDaemon(true);
            return t;
        });
        this.flushScheduler.scheduleAtFixedRate(this::flushBuffer, 5, 5, TimeUnit.SECONDS);
    }

    public static Builder builder(String baseUrl) {
        return new Builder(baseUrl);
    }

    public CompletableFuture<PublishResult> publishAsync(EventRecord event) {
        return CompletableFuture.supplyAsync(() -> publish(event));
    }

    public PublishResult publish(EventRecord event) {
        return publishBatch(List.of(event));
    }

    public CompletableFuture<PublishResult> publishBatchAsync(List<EventRecord> events) {
        return CompletableFuture.supplyAsync(() -> publishBatch(events));
    }

    public PublishResult publishBatch(List<EventRecord> events) {
        if (closed.get()) {
            throw new IllegalStateException("Producer is closed");
        }
        try {
            return sendWithRetry(events);
        } catch (Exception e) {
            buffer.addAll(events);
            log.log(Level.WARNING, "Network unavailable, buffered {0} events", events.size());
            return PublishResult.buffered(events.size());
        }
    }

    public int flushBuffer() {
        List<EventRecord> batch = new ArrayList<>();
        EventRecord event;
        while ((event = buffer.poll()) != null) {
            batch.add(event);
        }
        if (batch.isEmpty()) {
            return 0;
        }
        try {
            sendWithRetry(batch);
            log.log(Level.INFO, "Flushed {0} buffered events", batch.size());
            return batch.size();
        } catch (Exception e) {
            buffer.addAll(batch);
            log.log(Level.WARNING, "Flush failed, re-buffered {0} events", batch.size());
            return 0;
        }
    }

    private PublishResult sendWithRetry(List<EventRecord> events) throws Exception {
        Exception lastException = null;
        Duration backoff = initialBackoff;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(buildRequest(events),
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 202) {
                    return PublishResult.accepted(events.size(), response.headers().firstValue("X-Trace-Id").orElse(null));
                }
                if (response.statusCode() >= 400 && response.statusCode() < 500) {
                    throw new PublishException("Client error " + response.statusCode() + ": " + response.body());
                }
                lastException = new PublishException("Server error " + response.statusCode() + ": " + response.body());
            } catch (PublishException e) {
                throw e;
            } catch (Exception e) {
                lastException = e;
            }

            if (attempt < maxRetries) {
                Thread.sleep(backoff.toMillis());
                backoff = Duration.ofMillis((long) (backoff.toMillis() * backoffMultiplier));
            }
        }
        throw lastException != null ? lastException : new PublishException("Publish failed after retries");
    }

    private HttpRequest buildRequest(List<EventRecord> events) throws Exception {
        String body = events.size() == 1
                ? objectMapper.writeValueAsString(events.getFirst())
                : objectMapper.writeValueAsString(Map.of("events", events));

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/events"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("X-Trace-Id", UUID.randomUUID().toString())
                .POST(HttpRequest.BodyPublishers.ofString(body));

        if (apiKey != null) {
            builder.header("X-API-Key", apiKey);
        } else if (jwtToken != null) {
            builder.header("Authorization", "Bearer " + jwtToken);
        }
        return builder.build();
    }

    public int bufferedCount() {
        return buffer.size();
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            flushBuffer();
            flushScheduler.shutdown();
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record EventRecord(
            String eventType,
            Instant timestamp,
            String tenantId,
            JsonNode payload
    ) {
        public static EventRecord of(String eventType, String tenantId, Map<String, Object> payload) {
            ObjectMapper mapper = new ObjectMapper();
            return new EventRecord(eventType, Instant.now(), tenantId, mapper.valueToTree(payload));
        }
    }

    public record PublishResult(String status, int count, String traceId) {
        static PublishResult accepted(int count, String traceId) {
            return new PublishResult("ACCEPTED", count, traceId);
        }

        static PublishResult buffered(int count) {
            return new PublishResult("BUFFERED", count, null);
        }
    }

    public static class PublishException extends RuntimeException {
        public PublishException(String message) {
            super(message);
        }
    }

    public static class Builder {
        private final String baseUrl;
        private String apiKey;
        private String jwtToken;
        private int maxRetries = 3;
        private Duration initialBackoff = Duration.ofMillis(500);
        private double backoffMultiplier = 2.0;
        private Duration connectTimeout = Duration.ofSeconds(10);

        public Builder(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder jwtToken(String jwtToken) {
            this.jwtToken = jwtToken;
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder initialBackoff(Duration initialBackoff) {
            this.initialBackoff = initialBackoff;
            return this;
        }

        public Builder backoffMultiplier(double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
            return this;
        }

        public EventProducer build() {
            if (apiKey == null && jwtToken == null) {
                throw new IllegalArgumentException("Either apiKey or jwtToken is required");
            }
            return new EventProducer(this);
        }
    }
}
