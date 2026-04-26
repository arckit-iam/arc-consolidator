package com.instantaccessprovider.arcconsolidator.kafka;

import com.instantaccessprovider.arcconsolidator.domain.port.ArcEventOutputPort;
import com.instantaccessprovider.arcconsolidator.model.ArcEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Adapter Kafka implémentant le port de sortie ARC.
 * Publie les événements ARC normalisés sur le topic configuré.
 */
@Component
public class ArcEventPublisher implements ArcEventOutputPort {

    private static final Logger log = LoggerFactory.getLogger(ArcEventPublisher.class);

    private final KafkaTemplate<String, ArcEvent> kafkaTemplate;
    private final String topic;

    public ArcEventPublisher(KafkaTemplate<String, ArcEvent> kafkaTemplate,
                             @Value("${arc.topic:arc}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(ArcEvent event) {
        String key = event.getTarget() != null ? event.getTarget().getAccountName() : null;
        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish ARC event id={}: {}", event.getId(), ex.getMessage(), ex);
                    } else {
                        log.debug("ARC event id={} published to topic={} partition={} offset={}",
                                event.getId(), topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
