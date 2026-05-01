package com.instantaccessprovider.arcconsolidator.model;

import java.time.OffsetDateTime;

/**
 * Modèle cible ARC pour les événements consolidés (source Active Directory).
 * Représente un événement de cycle de vie d'un compte utilisateur normalisé.
 */
import java.util.List;
import java.util.ArrayList;

public class ArcEvent {

    private String id;
    private OffsetDateTime timestamp;
    private ArcEventType eventType;
    private String source;
    private String sourceComputer;
    private int sourceEventId;
    private Actor actor;
    private Target target;
    /** Liste des attributs modifiés (pour les events de modification) */
    private List<ChangedAttribute> changedAttributes;
    /**
     * Représente un attribut modifié lors d'un event de modification (ex: 4738).
     */
    public static class ChangedAttribute {
        private String name;
        private String oldValue;
        private String newValue;

        public ChangedAttribute() {}
        public ChangedAttribute(String name, String oldValue, String newValue) {
            this.name = name;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getOldValue() { return oldValue; }
        public void setOldValue(String oldValue) { this.oldValue = oldValue; }
        public String getNewValue() { return newValue; }
        public void setNewValue(String newValue) { this.newValue = newValue; }
    }
    public List<ChangedAttribute> getChangedAttributes() { return changedAttributes; }
    public void setChangedAttributes(List<ChangedAttribute> changedAttributes) { this.changedAttributes = changedAttributes; }

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
    // Ajout pour ASSIGNMENT: exposer explicitement identity et entitlement
    private Identity identity;
    private Entitlement entitlement;

    public Identity getIdentity() { return identity; }
    public void setIdentity(Identity identity) { this.identity = identity; }

    public Entitlement getEntitlement() { return entitlement; }
    public void setEntitlement(Entitlement entitlement) { this.entitlement = entitlement; }

    /**
     * Représente une identité (user/service/machine) pour les events d'assignation.
     */
    public static class Identity {
        private String id;
        private String displayName;
        private String externalId;
        private String type;
        private String sourceSystem;
        // Ajoute d'autres champs métier si besoin

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getExternalId() { return externalId; }
        public void setExternalId(String externalId) { this.externalId = externalId; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getSourceSystem() { return sourceSystem; }
        public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }
    }

    /**
     * Représente un entitlement (groupe/profil/rôle) pour les events d'assignation.
     */
    public static class Entitlement {
        private String id;
        private String name;
        private String externalId;
        private String type;
        private String sourceSystem;
        // Ajoute d'autres champs métier si besoin

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getExternalId() { return externalId; }
        public void setExternalId(String externalId) { this.externalId = externalId; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getSourceSystem() { return sourceSystem; }
        public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }
    }
}
