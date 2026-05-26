package com.drep.ingestion.config;

import com.drep.common.model.Event;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(KafkaProperties.class)
@ConditionalOnProperty(prefix = "drep.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaProducerConfig {

    @Bean
    public KafkaAdmin kafkaAdmin(org.springframework.boot.autoconfigure.kafka.KafkaProperties springKafkaProperties) {
        return new KafkaAdmin(springKafkaProperties.buildAdminProperties(null));
    }

    @Bean
    public NewTopic rawEventsTopic(KafkaProperties kafkaProperties) {
        return topic(kafkaProperties.getTopics().getRaw(), kafkaProperties.getTopics());
    }

    @Bean
    public NewTopic processedEventsTopic(KafkaProperties kafkaProperties) {
        return topic(kafkaProperties.getTopics().getProcessed(), kafkaProperties.getTopics());
    }

    @Bean
    public NewTopic dlqEventsTopic(KafkaProperties kafkaProperties) {
        return topic(kafkaProperties.getTopics().getDlq(), kafkaProperties.getTopics());
    }

    private NewTopic topic(String name, KafkaProperties.Topics topics) {
        return new NewTopic(name, topics.getPartitions(), topics.getReplicationFactor())
                .configs(Map.of("retention.ms", String.valueOf(topics.getRetentionMs())));
    }

    @Bean
    public ProducerFactory<String, Event> eventProducerFactory(
            org.springframework.boot.autoconfigure.kafka.KafkaProperties springKafkaProperties,
            KafkaProperties kafkaProperties) {
        Map<String, Object> config = new HashMap<>(springKafkaProperties.buildProducerProperties(null));
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, kafkaProperties.getProducer().getAcks());
        config.put(ProducerConfig.RETRIES_CONFIG, kafkaProperties.getProducer().getRetries());
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Event> eventKafkaTemplate(ProducerFactory<String, Event> eventProducerFactory) {
        return new KafkaTemplate<>(eventProducerFactory);
    }
}
