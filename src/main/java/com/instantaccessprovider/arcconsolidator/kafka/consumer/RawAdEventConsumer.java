package com.instantaccessprovider.arcconsolidator.kafka.consumer;

import com.instantaccessprovider.arcconsolidator.application.service.ArcConsolidatorService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Adapter Kafka : consomme les événements bruts Active Directory depuis iam.raw.activedirectory
 * et délègue la consolidation au service applicatif AD.
 */
@Component
public class RawAdEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(RawAdEventConsumer.class);

    private final ArcConsolidatorService service;

    public RawAdEventConsumer(ArcConsolidatorService service) {
        this.service = service;
    }

    @KafkaListener(
            topics = "${iam.topics.activedirectory:iam.raw.activedirectory}",
            groupId = "${spring.kafka.consumer.group-id:arc-consolidator}"
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        log.debug("Received raw AD event: topic={}, partition={}, offset={}",
                record.topic(), record.partition(), record.offset());
        service.consolidate("activedirectory", record.value());
    }
}
