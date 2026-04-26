package com.instantaccessprovider.arcconsolidator.model;

import java.time.OffsetDateTime;

/**
 * Modèle cible ARC pour les événements consolidés (source Active Directory).
 * Représente un événement de cycle de vie d'un compte utilisateur normalisé.
 */
public class ArcEvent {

    private String id;
    private OffsetDateTime timestamp;
    private ArcEventType eventType;
    private String source;
    private String sourceComputer;
    private int sourceEventId;
    private Actor actor;
    private Target target;

    /**
     * Compte ayant réalisé l'action (sujet de l'événement Windows).
     */
    public static class Actor {
        private String securityId;
        private String accountName;
        private String domain;
        private String logonId;

        public String getSecurityId() { return securityId; }
        public void setSecurityId(String securityId) { this.securityId = securityId; }

        public String getAccountName() { return accountName; }
        public void setAccountName(String accountName) { this.accountName = accountName; }

        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }

        public String getLogonId() { return logonId; }
        public void setLogonId(String logonId) { this.logonId = logonId; }
    }

    /**
     * Compte cible de l'action.
     */
    public static class Target {
        private String securityId;
        private String accountName;
        private String domain;

        public String getSecurityId() { return securityId; }
        public void setSecurityId(String securityId) { this.securityId = securityId; }

        public String getAccountName() { return accountName; }
        public void setAccountName(String accountName) { this.accountName = accountName; }

        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public OffsetDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }

    public ArcEventType getEventType() { return eventType; }
    public void setEventType(ArcEventType eventType) { this.eventType = eventType; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getSourceComputer() { return sourceComputer; }
    public void setSourceComputer(String sourceComputer) { this.sourceComputer = sourceComputer; }

    public int getSourceEventId() { return sourceEventId; }
    public void setSourceEventId(int sourceEventId) { this.sourceEventId = sourceEventId; }

    public Actor getActor() { return actor; }
    public void setActor(Actor actor) { this.actor = actor; }

    public Target getTarget() { return target; }
    public void setTarget(Target target) { this.target = target; }
}
