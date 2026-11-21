// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcconsolidator.kafka.consumer;

import com.instantaccessprovider.arcconsolidator.application.service.IsdsConsolidatorService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Adapter Kafka : consomme les événements bruts ISDS depuis iam.raw.isds
 * et délègue la consolidation au service applicatif dédié ISDS.
 */
@Component
public class RawIsdsEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(RawIsdsEventConsumer.class);

    private final IsdsConsolidatorService service;

    public RawIsdsEventConsumer(IsdsConsolidatorService service) {
        this.service = service;
    }

    @KafkaListener(
            topics = "${iam.topics.isds:iam.raw.isds}",
            groupId = "${spring.kafka.consumer.group-id:arc-consolidator}"
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        log.debug("Received raw ISDS event: topic={}, partition={}, offset={}",
                record.topic(), record.partition(), record.offset());
        service.consolidate("isds", record.value());
    }
}
