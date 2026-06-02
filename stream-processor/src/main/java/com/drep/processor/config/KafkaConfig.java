package com.drep.processor.config;

import com.drep.common.model.DlqEvent;
import com.drep.common.model.Event;
import com.drep.common.model.ProcessedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaConfig {

    @Bean
    public ConsumerFactory<String, Event> rawEventConsumerFactory(
            org.springframework.boot.autoconfigure.kafka.KafkaProperties springKafkaProperties,
            KafkaProperties kafkaProperties) {
        Map<String, Object> config = new HashMap<>(springKafkaProperties.buildConsumerProperties(null));
        config.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaProperties.getRawConsumer().getGroupId());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.drep.common.model");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Event.class.getName());
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Event> rawEventKafkaListenerContainerFactory(
            ConsumerFactory<String, Event> rawEventConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Event> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(rawEventConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }

    @Bean
    public ProducerFactory<String, ProcessedEvent> processedEventProducerFactory(
            org.springframework.boot.autoconfigure.kafka.KafkaProperties springKafkaProperties) {
        Map<String, Object> config = new HashMap<>(springKafkaProperties.buildProducerProperties(null));
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, ProcessedEvent> processedEventKafkaTemplate(
            ProducerFactory<String, ProcessedEvent> processedEventProducerFactory) {
        return new KafkaTemplate<>(processedEventProducerFactory);
    }

    @Bean
    public ProducerFactory<String, Event> eventProducerFactory(
            org.springframework.boot.autoconfigure.kafka.KafkaProperties springKafkaProperties) {
        Map<String, Object> config = new HashMap<>(springKafkaProperties.buildProducerProperties(null));
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Event> eventKafkaTemplate(ProducerFactory<String, Event> eventProducerFactory) {
        return new KafkaTemplate<>(eventProducerFactory);
    }

    @Bean
    public ProducerFactory<String, DlqEvent> dlqProducerFactory(
            org.springframework.boot.autoconfigure.kafka.KafkaProperties springKafkaProperties) {
        Map<String, Object> config = new HashMap<>(springKafkaProperties.buildProducerProperties(null));
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, DlqEvent> dlqKafkaTemplate(ProducerFactory<String, DlqEvent> dlqProducerFactory) {
        return new KafkaTemplate<>(dlqProducerFactory);
    }
}
