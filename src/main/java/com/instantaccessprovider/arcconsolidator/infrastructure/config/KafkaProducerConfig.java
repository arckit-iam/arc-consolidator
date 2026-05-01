package com.instantaccessprovider.arcconsolidator.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.instantaccessprovider.arcconsolidator.model.ArcEvent;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

/**
 * Configuration de l'infrastructure Kafka pour le producteur ARC.
 * Utilise un ObjectMapper avec JavaTimeModule pour sérialiser correctement OffsetDateTime.
 */
@Configuration
public class KafkaProducerConfig {

        // --- Bean générique pour injection dans ArcEventPublisher ---
        @Bean
        public ProducerFactory<String, Object> objectProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = kafkaProperties.buildProducerProperties(null);
        ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new DefaultKafkaProducerFactory<>(
            props,
            new StringSerializer(),
            new JsonSerializer<>(mapper));
        }

        @Bean
        public KafkaTemplate<String, Object> kafkaObjectTemplate(
            ProducerFactory<String, Object> objectProducerFactory) {
        return new KafkaTemplate<>(objectProducerFactory);
        }
}
