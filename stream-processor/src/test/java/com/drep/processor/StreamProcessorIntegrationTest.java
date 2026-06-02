package com.drep.processor;

import com.drep.common.model.DlqEvent;
import com.drep.common.model.Event;
import com.drep.common.model.ProcessedEvent;
import com.drep.processor.idempotency.InMemoryIdempotencyService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"events.raw", "events.processed", "events.dlq"})
@ActiveProfiles("test")
@DirtiesContext
class StreamProcessorIntegrationTest {

    @Autowired
    private InMemoryIdempotencyService inMemoryIdempotencyService;

    @BeforeEach
    void setUp() {
        inMemoryIdempotencyService.clear();
    }

    @Test
    void successfulEventIsPublishedToProcessedTopicWithEnrichment() throws Exception {
        Event event = sampleEvent("Page.View");
        publishRaw(event);

        ProcessedEvent processed = pollProcessed(event.eventId());
        assertThat(processed).isNotNull();
        assertThat(processed.eventType()).isEqualTo("page.view");
        assertThat(processed.schemaVersion()).isEqualTo(1);
        assertThat(processed.processedAt()).isNotNull();
        assertThat(processed.enrichment()).isNotNull();
        assertThat(processed.enrichment().get("tenantTier").asText()).isEqualTo("premium");
    }

    @Test
    void duplicateEventIsProcessedExactlyOnce() throws Exception {
        Event event = sampleEvent("page.view");
        publishRaw(event);
        publishRaw(event);

        int count = countProcessed(event.eventId());
        assertThat(count).isEqualTo(1);
    }

    @Test
    void failedEventIsRoutedToDlqAfterRetries() throws Exception {
        Event event = sampleEvent("fail.process");
        publishRaw(event);

        DlqEvent dlqEvent = pollDlq(event.eventId());
        assertThat(dlqEvent).isNotNull();
        assertThat(dlqEvent.originalEvent().eventId()).isEqualTo(event.eventId());
        assertThat(dlqEvent.retryCount()).isEqualTo(3);
        assertThat(dlqEvent.failureReason()).contains("Forced processing failure");
        assertThat(dlqEvent.sourceTopic()).isEqualTo("events.raw");
        assertThat(dlqEvent.consumerGroup()).isEqualTo("stream-processor-group");
    }

    private Event sampleEvent(String eventType) {
        return new Event(
                UUID.randomUUID(),
                eventType,
                Instant.now(),
                "tenant-a",
                null,
                "trace-1",
                Instant.now()
        );
    }

    private void publishRaw(Event event) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getProperty("spring.embedded.kafka.brokers"));
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        KafkaTemplate<String, Event> template = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(config));
        template.send(new ProducerRecord<>("events.raw", "tenant-a:" + event.eventType(), event));
        template.flush();
        template.destroy();
    }

    private ProcessedEvent pollProcessed(UUID eventId) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (System.nanoTime() < deadline) {
            try (KafkaConsumer<String, ProcessedEvent> consumer = processedConsumer("processed-test")) {
                consumer.subscribe(Collections.singletonList("events.processed"));
                ConsumerRecords<String, ProcessedEvent> records = consumer.poll(Duration.ofMillis(500));
                for (var record : records) {
                    if (record.value().eventId().equals(eventId)) {
                        return record.value();
                    }
                }
            }
            Thread.sleep(300);
        }
        return null;
    }

    private int countProcessed(UUID eventId) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        int count = 0;
        while (System.nanoTime() < deadline) {
            try (KafkaConsumer<String, ProcessedEvent> consumer = processedConsumer("processed-count-test")) {
                consumer.subscribe(Collections.singletonList("events.processed"));
                ConsumerRecords<String, ProcessedEvent> records = consumer.poll(Duration.ofMillis(500));
                for (var record : records) {
                    if (record.value().eventId().equals(eventId)) {
                        count++;
                    }
                }
            }
            if (count > 0) {
                Thread.sleep(1000);
                return count;
            }
            Thread.sleep(300);
        }
        return count;
    }

    private DlqEvent pollDlq(UUID eventId) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (System.nanoTime() < deadline) {
            try (KafkaConsumer<String, DlqEvent> consumer = dlqConsumer()) {
                consumer.subscribe(Collections.singletonList("events.dlq"));
                ConsumerRecords<String, DlqEvent> records = consumer.poll(Duration.ofMillis(500));
                for (var record : records) {
                    if (record.value().originalEvent().eventId().equals(eventId)) {
                        return record.value();
                    }
                }
            }
            Thread.sleep(300);
        }
        return null;
    }

    private KafkaConsumer<String, ProcessedEvent> processedConsumer(String groupId) {
        return new KafkaConsumer<>(consumerProps(groupId, ProcessedEvent.class));
    }

    private KafkaConsumer<String, DlqEvent> dlqConsumer() {
        return new KafkaConsumer<>(consumerProps("dlq-test", DlqEvent.class));
    }

    private Properties consumerProps(String groupId, Class<?> valueType) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getProperty("spring.embedded.kafka.brokers"));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.drep.common.model");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, valueType.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return props;
    }
}
