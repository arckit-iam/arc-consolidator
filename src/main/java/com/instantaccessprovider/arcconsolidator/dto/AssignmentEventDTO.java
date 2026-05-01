package com.instantaccessprovider.arcconsolidator.dto;

import java.time.OffsetDateTime;
import java.util.List;

public class AssignmentEventDTO {
    private String id;
    private OffsetDateTime timestamp;
    private String eventType;
    private String source;
    private String sourceComputer;
    private int sourceEventId;
    private Identity identity;
    private Entitlement entitlement;
    private List<ChangedAttribute> changedAttributes;

    public static class Identity {
        private String displayName;
        private String externalId;
        private String type;
        private String sourceSystem;
        // getters/setters
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getExternalId() { return externalId; }
        public void setExternalId(String externalId) { this.externalId = externalId; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getSourceSystem() { return sourceSystem; }
        public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }
    }
    public static class Entitlement {
        private String name;
        private String externalId;
        private String type;
        private String sourceSystem;
        // getters/setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getExternalId() { return externalId; }
        public void setExternalId(String externalId) { this.externalId = externalId; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getSourceSystem() { return sourceSystem; }
        public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }
    }
    public static class ChangedAttribute {
        private String name;
        private String oldValue;
        private String newValue;
        // getters/setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getOldValue() { return oldValue; }
        public void setOldValue(String oldValue) { this.oldValue = oldValue; }
        public String getNewValue() { return newValue; }
        public void setNewValue(String newValue) { this.newValue = newValue; }
    }
    // getters/setters pour tous les champs
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public OffsetDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getSourceComputer() { return sourceComputer; }
    public void setSourceComputer(String sourceComputer) { this.sourceComputer = sourceComputer; }
    public int getSourceEventId() { return sourceEventId; }
    public void setSourceEventId(int sourceEventId) { this.sourceEventId = sourceEventId; }
    public Identity getIdentity() { return identity; }
    public void setIdentity(Identity identity) { this.identity = identity; }
    public Entitlement getEntitlement() { return entitlement; }
    public void setEntitlement(Entitlement entitlement) { this.entitlement = entitlement; }
    public List<ChangedAttribute> getChangedAttributes() { return changedAttributes; }
    public void setChangedAttributes(List<ChangedAttribute> changedAttributes) { this.changedAttributes = changedAttributes; }
}
