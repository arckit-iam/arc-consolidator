
package com.instantaccessprovider.arcconsolidator.application.service;

import org.springframework.stereotype.Service;

/**
 * Service métier pour la consolidation des événements ISDS.
 * À implémenter lors de l’ajout du mapping et de la logique métier ISDS.
 */
@Service
public class IsdsConsolidatorService {
    // TODO: Injecter les dépendances nécessaires (mappers, ports, etc.)

    public void consolidate(String rawEvent) {
        // TODO: Mapper et traiter l’événement ISDS brut
    }

    // Surcharge pour compatibilité avec RawIsdsEventConsumer
    public void consolidate(String source, String rawEvent) {
        consolidate(rawEvent);
    }
}
