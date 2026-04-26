package com.instantaccessprovider.arcconsolidator.domain.port;

import com.instantaccessprovider.arcconsolidator.model.ArcEvent;

/**
 * Port de sortie : publication d'un événement ARC normalisé.
 * Implémenté par l'adapter Kafka dans l'infrastructure.
 */
public interface ArcEventOutputPort {
    void publish(ArcEvent event);
}
