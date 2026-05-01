package com.instantaccessprovider.arcconsolidator.kafka;

import com.instantaccessprovider.arcconsolidator.domain.port.ArcEventOutputPort;
import com.instantaccessprovider.arcconsolidator.model.ArcEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
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
    private final String defaultTopic;
    private final String identityTopic;
    private final String entitlementTopic;
    private final String assignmentTopic;

    @Autowired
    public ArcEventPublisher(KafkaTemplate<String, ArcEvent> kafkaTemplate, Environment env) {
        this.kafkaTemplate = kafkaTemplate;
        this.defaultTopic = env.getProperty("arc.topic", "arc");
        this.identityTopic = env.getProperty("arc.topics.identity", "arc.identity");
        this.entitlementTopic = env.getProperty("arc.topics.entitlement", "arc.entitlement");
        this.assignmentTopic = env.getProperty("arc.topics.assignment", "arc.assignment");
    }

    @Override
    public void publish(ArcEvent event) {
        String key = event.getTarget() != null ? event.getTarget().getAccountName() : null;
        String topic = resolveTopic(event);
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

    private String resolveTopic(ArcEvent event) {
        switch (event.getEventType()) {
            case USER_CREATED:
            case USER_ENABLED:
            case USER_DISABLED:
            case USER_DELETED:
            case USER_MODIFIED:
            case USER_RENAMED:
                return identityTopic;
            case GROUP_CREATED:
            case GROUP_MODIFIED:
            case GROUP_DELETED:
                return entitlementTopic;
            case ASSIGNMENT_ADDED:
            case ASSIGNMENT_REMOVED:
                return assignmentTopic;
            default:
                return defaultTopic;
        }
    }
}
