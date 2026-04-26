package com.instantaccessprovider.arcconsolidator.application.service;

import com.instantaccessprovider.arcconsolidator.domain.port.ArcEventOutputPort;
import com.instantaccessprovider.arcconsolidator.mapper.ArcEventMapper;
import com.instantaccessprovider.arcconsolidator.model.ArcEvent;
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

    private final ArcEventMapper mapper;
    private final ArcEventOutputPort outputPort;

    public ArcConsolidatorService(ArcEventMapper mapper, ArcEventOutputPort outputPort) {
        this.mapper = mapper;
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
            ArcEvent event = mapper.fromRaw(rawPayload);
            outputPort.publish(event);
            log.info("ARC event published: source={}, type={}, target={}",
                    source,
                    event.getEventType(),
                    event.getTarget() != null ? event.getTarget().getAccountName() : "unknown");
        } catch (Exception e) {
            log.error("Failed to consolidate event from source={}: {}", source, e.getMessage(), e);
        }
    }
}
