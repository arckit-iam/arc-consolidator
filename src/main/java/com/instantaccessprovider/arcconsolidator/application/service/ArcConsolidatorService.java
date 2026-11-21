// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcconsolidator.application.service;

import com.instantaccessprovider.arcconsolidator.domain.port.ArcEventOutputPort;
import com.instantaccessprovider.arcconsolidator.mapper.ActiveDirectoryEventMapper;
import com.instantaccessprovider.arcconsolidator.dto.IdentityEventDTO;
import com.instantaccessprovider.arcconsolidator.dto.EntitlementEventDTO;
import com.instantaccessprovider.arcconsolidator.dto.AssignmentEventDTO;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service applicatif du use case de consolidation ARC.
 * Orchestre la transformation d'un événement brut et sa publication sur le topic cible.
 */
@Service
public class ArcConsolidatorService {

    private static final Logger log = LoggerFactory.getLogger(ArcConsolidatorService.class);

    private final ActiveDirectoryEventMapper adMapper;
    private final ArcEventOutputPort outputPort;

    public ArcConsolidatorService(ActiveDirectoryEventMapper adMapper, ArcEventOutputPort outputPort) {
        this.adMapper = adMapper;
        this.outputPort = outputPort;
    }

    /**
     * Consolidate un événement brut depuis une source IAM et publie l'événement ARC normalisé.
     *
     * @param source     identifiant de la source (ex : "activedirectory")
     * @param rawPayload payload JSON brut reçu depuis le topic Kafka
     */
    public void consolidate(String source, String rawPayload) {
        try {
            int eventId = extractEventId(rawPayload);
            if (isIdentityEvent(eventId)) {
                IdentityEventDTO dto = adMapper.fromRawToIdentityEventDTO(rawPayload);
                outputPort.publishIdentity(dto);
                log.info("Identity event published: source={}, type={}, displayName={}",
                        source, dto.getEventType(), dto.getDisplayName());
            } else if (isEntitlementEvent(eventId)) {
                EntitlementEventDTO dto = adMapper.fromRawToEntitlementEventDTO(rawPayload);
                outputPort.publishEntitlement(dto);
                log.info("Entitlement event published: source={}, type={}, name={}",
                        source, dto.getEventType(), dto.getName());
            } else if (isAssignmentEvent(eventId)) {
                AssignmentEventDTO dto = adMapper.fromRawToAssignmentEventDTO(rawPayload);
                outputPort.publishAssignment(dto);
                log.info("Assignment event published: source={}, type={}, identity={}",
                        source, dto.getEventType(), dto.getIdentity() != null ? dto.getIdentity().getDisplayName() : "null");
            } else {
                log.warn("EventID {} not mapped to a known DTO type, skipping", eventId);
            }
        } catch (Exception e) {
            log.error("Failed to consolidate event from source={}: {}", source, e.getMessage(), e);
        }
    }

    private int extractEventId(String rawPayload) throws IOException {
        JsonNode node = adMapper.objectMapper.readTree(rawPayload);
        return node.path("EventID").asInt();
    }

    private boolean isIdentityEvent(int eventId) {
        return eventId == 4720 || eventId == 4722 || eventId == 4725 || eventId == 4726 || eventId == 4738 || eventId == 4781;
    }

    private boolean isEntitlementEvent(int eventId) {
        return eventId == 4727 || eventId == 4754 || eventId == 4730 || eventId == 4734 || eventId == 4758 || eventId == 4731;
    }

    private boolean isAssignmentEvent(int eventId) {
        return eventId == 4728 || eventId == 4732 || eventId == 4756 || eventId == 4729 || eventId == 4733 || eventId == 4757;
    }
}
