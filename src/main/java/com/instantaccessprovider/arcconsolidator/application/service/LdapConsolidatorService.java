// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcconsolidator.application.service;

import com.instantaccessprovider.arcconsolidator.domain.port.ArcEventOutputPort;
import com.instantaccessprovider.arcconsolidator.mapper.LdapEventMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Service applicatif de consolidation des changements LDAP : transforme un message du topic
 * {@code iam.raw.ldap} en événements ARC normalisés et les publie.
 *
 * <p>L'agent de collecte livre au moins une fois : après un redémarrage, il peut republier le dernier
 * changement déjà envoyé, avec le même identifiant. Les identifiants récents sont mémorisés pour écarter
 * ces doublons.
 */
@Service
public class LdapConsolidatorService {

    private static final Logger log = LoggerFactory.getLogger(LdapConsolidatorService.class);
    private static final int RECENT_IDS = 10_000;

    private final LdapEventMapper mapper;
    private final ArcEventOutputPort outputPort;
    private final Set<String> recentIds = Collections.newSetFromMap(new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > RECENT_IDS;
        }
    });

    public LdapConsolidatorService(LdapEventMapper mapper, ArcEventOutputPort outputPort) {
        this.mapper = mapper;
        this.outputPort = outputPort;
    }

    /**
     * @param source     identifiant de la source (ex : "ldap")
     * @param rawPayload message JSON brut reçu depuis le topic Kafka
     */
    public void consolidate(String source, String rawPayload) {
        try {
            String id = mapper.extractId(rawPayload);
            if (id != null && !firstSeen(id)) {
                log.debug("Changement LDAP deja traite, ignore: id={}", id);
                return;
            }
            LdapEventMapper.Result result = mapper.map(rawPayload);
            if (result.isEmpty()) {
                log.debug("Changement LDAP sans evenement ARC associe: id={}", id);
                return;
            }
            result.identities().forEach(dto -> {
                outputPort.publishIdentity(dto);
                log.info("Identity event published: source={}, type={}, name={}, origin={}",
                        source, dto.getEventType(), dto.getName(), dto.getOrigin());
            });
            result.entitlements().forEach(dto -> {
                outputPort.publishEntitlement(dto);
                log.info("Entitlement event published: source={}, type={}, name={}, origin={}",
                        source, dto.getEventType(), dto.getName(), dto.getOrigin());
            });
            result.assignments().forEach(dto -> {
                outputPort.publishAssignment(dto);
                log.info("Assignment event published: source={}, type={}, identity={}, entitlement={}, origin={}",
                        source, dto.getEventType(), dto.getIdentity().getDisplayName(),
                        dto.getEntitlement().getName(), dto.getOrigin());
            });
        } catch (Exception e) {
            log.error("Failed to consolidate event from source={}: {}", source, e.getMessage(), e);
        }
    }

    private synchronized boolean firstSeen(String id) {
        return recentIds.add(id);
    }
}
