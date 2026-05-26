package com.drep.ingestion.kafka;

import com.drep.common.model.Event;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"events.raw", "events.processed", "events.dlq"})
@ActiveProfiles("kafka-test")
@DirtiesContext
class KafkaPublisherIntegrationTest {

    @Autowired
    private EventKafkaPublisher eventKafkaPublisher;

    @Test
    void publishesEventToRawTopic() throws Exception {
        Event event = new Event(
                UUID.randomUUID(),
                "page.view",
                Instant.now(),
                "tenant-a",
                null,
                "trace-1",
                Instant.now()
        );

        eventKafkaPublisher.publish(event);

        Event consumed = null;
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            try (KafkaConsumer<String, Event> consumer = createConsumer()) {
                consumer.subscribe(Collections.singletonList("events.raw"));
                ConsumerRecords<String, Event> records = consumer.poll(Duration.ofMillis(500));
                if (!records.isEmpty()) {
                    consumed = records.iterator().next().value();
                    break;
                }
            }
            Thread.sleep(200);
        }

        assertThat(consumed).isNotNull();
        assertThat(consumed.eventId()).isEqualTo(event.eventId());
    }

    private KafkaConsumer<String, Event> createConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getProperty("spring.embedded.kafka.brokers"));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-publisher-test");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.drep.common.model");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Event.class.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new KafkaConsumer<>(props);
    }
}
