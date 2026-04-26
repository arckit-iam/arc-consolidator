package com.instantaccessprovider.arcconsolidator.model;

import java.time.OffsetDateTime;

/**
 * Modèle cible ARC pour les événements consolidés.
 */
public class ArcEvent {
    private OffsetDateTime eventTime;
    private String eventType;
    private String source;
    private User user;
    // ... autres champs selon le modèle ARC

    public static class User {
        public String accountName;
        public String domain;
        // ... autres champs utilisateur
    }

    // Getters/setters/constructeurs omis pour la concision
}
