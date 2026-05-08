package com.instantaccessprovider.arcconsolidator.kafka.publisher;

import com.instantaccessprovider.arcconsolidator.domain.port.ArcEventOutputPort;
import com.instantaccessprovider.arcconsolidator.dto.IdentityEventDTO;
import com.instantaccessprovider.arcconsolidator.dto.EntitlementEventDTO;
import com.instantaccessprovider.arcconsolidator.dto.AssignmentEventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String defaultTopic;
    private final String identityTopic;
    private final String entitlementTopic;
    private final String assignmentTopic;

    @Autowired
    public ArcEventPublisher(KafkaTemplate<String, Object> kafkaTemplate, Environment env) {
        this.kafkaTemplate = kafkaTemplate;
        this.defaultTopic = env.getProperty("arc.topic", "arc");
        this.identityTopic = env.getProperty("arc.topics.identity", "arc.sync.identity");
        this.entitlementTopic = env.getProperty("arc.topics.entitlement", "arc.sync.entitlement");
        this.assignmentTopic = env.getProperty("arc.topics.assignment", "arc.sync.assignment");
    }

    @Override
    public void publishIdentity(IdentityEventDTO event) {
        String key = event.getDisplayName();
        kafkaTemplate.send(identityTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish Identity event id={}: {}", event.getId(), ex.getMessage(), ex);
                    } else {
                        log.debug("Identity event id={} published to topic={} partition={} offset={}",
                                event.getId(), identityTopic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    @Override
    public void publishEntitlement(EntitlementEventDTO event) {
        String key = event.getName();
        kafkaTemplate.send(entitlementTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish Entitlement event id={}: {}", event.getId(), ex.getMessage(), ex);
                    } else {
                        log.debug("Entitlement event id={} published to topic={} partition={} offset={}",
                                event.getId(), entitlementTopic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    @Override
    public void publishAssignment(AssignmentEventDTO event) {
        String identityName = event.getIdentity() != null
                ? (event.getIdentity().getDisplayName() != null ? event.getIdentity().getDisplayName() : event.getIdentity().getName())
                : null;
        String entitlementName = event.getEntitlement() != null ? event.getEntitlement().getName() : null;
        String key = (identityName != null && entitlementName != null)
                ? identityName + "_" + entitlementName
                : (identityName != null ? identityName : entitlementName);
        kafkaTemplate.send(assignmentTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish Assignment event id={}: {}", event.getId(), ex.getMessage(), ex);
                    } else {
                        log.debug("Assignment event id={} published to topic={} partition={} offset={}",
                                event.getId(), assignmentTopic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
