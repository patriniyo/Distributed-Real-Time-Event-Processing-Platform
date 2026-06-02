package com.drep.analytics.config;

import com.drep.common.model.ProcessedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@EnableConfigurationProperties(AnalyticsProperties.class)
public class KafkaConfig {

    @Bean
    public ConsumerFactory<String, ProcessedEvent> processedEventConsumerFactory(
            org.springframework.boot.autoconfigure.kafka.KafkaProperties springKafkaProperties,
            AnalyticsProperties analyticsProperties) {
        Map<String, Object> config = new HashMap<>(springKafkaProperties.buildConsumerProperties(null));
        config.put(ConsumerConfig.GROUP_ID_CONFIG, analyticsProperties.getKafka().getConsumerGroup());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.drep.common.model");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ProcessedEvent.class.getName());
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ProcessedEvent> processedEventKafkaListenerContainerFactory(
            ConsumerFactory<String, ProcessedEvent> processedEventConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, ProcessedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(processedEventConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
}
