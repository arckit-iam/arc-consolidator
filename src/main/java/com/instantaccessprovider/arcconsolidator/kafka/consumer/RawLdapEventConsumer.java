// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcconsolidator.kafka.consumer;

import com.instantaccessprovider.arcconsolidator.application.service.LdapConsolidatorService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Adapter Kafka : consomme les changements bruts LDAP depuis iam.raw.ldap
 * et délègue la consolidation au service applicatif LDAP.
 */
@Component
public class RawLdapEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(RawLdapEventConsumer.class);

    private final LdapConsolidatorService service;

    public RawLdapEventConsumer(LdapConsolidatorService service) {
        this.service = service;
    }

    @KafkaListener(
            topics = "${iam.topics.ldap:iam.raw.ldap}",
            groupId = "${spring.kafka.consumer.group-id:arc-consolidator}"
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        log.debug("Received raw LDAP event: topic={}, partition={}, offset={}",
                record.topic(), record.partition(), record.offset());
        service.consolidate("ldap", record.value());
    }
}
