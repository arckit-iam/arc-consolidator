// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcconsolidator.dto;

import java.time.OffsetDateTime;
import java.util.List;

public class AssignmentEventDTO {
    private String id;
    private OffsetDateTime timestamp;
    private String eventType;
    private String source;
    private String system;
    private int sourceEventId;
    private Identity identity;
    private Entitlement entitlement;
    private Scope scope;
    /** Origine du changement quand la source la fournit (ex : arc-agent, external). */
    private String origin;
    private List<ChangedAttribute> changedAttributes;

    public static class Identity {
        private String name;        // CN=... / DN
        private String displayName; // nom lisible (sAMAccountName)
        private String externalId;  // SID
        private String type;
        private String system;
        // getters/setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getExternalId() { return externalId; }
        public void setExternalId(String externalId) { this.externalId = externalId; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getSystem() { return system; }
        public void setSystem(String system) { this.system = system; }
    }
    public static class Entitlement {
        private String name;        // CN=... / DN (ou sAMAccountName si DN absent)
        private String displayName; // nom lisible (sAMAccountName)
        private String externalId;  // SID
        private String type;
        private String system;
        // getters/setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getExternalId() { return externalId; }
        public void setExternalId(String externalId) { this.externalId = externalId; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getSystem() { return system; }
        public void setSystem(String system) { this.system = system; }
    }
    public static class Scope {
        /** Dimensions du scope (ex: géographique, organisationnel, applicatif...) */
        private java.util.List<Dimension> dimensions;
        public java.util.List<Dimension> getDimensions() { return dimensions; }
        public void setDimensions(java.util.List<Dimension> dimensions) { this.dimensions = dimensions; }

        public static class Dimension {
            private String type;   // ex: "geography", "org-unit", "application"
            private String value;  // ex: "EMEA", "FIN-DEPT"
            private String label;  // libellé lisible
            public String getType() { return type; }
            public void setType(String type) { this.type = type; }
            public String getValue() { return value; }
            public void setValue(String value) { this.value = value; }
            public String getLabel() { return label; }
            public void setLabel(String label) { this.label = label; }
        }
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
    public String getSystem() { return system; }
    public void setSystem(String system) { this.system = system; }
    public int getSourceEventId() { return sourceEventId; }
    public void setSourceEventId(int sourceEventId) { this.sourceEventId = sourceEventId; }
    public Identity getIdentity() { return identity; }
    public void setIdentity(Identity identity) { this.identity = identity; }
    public Entitlement getEntitlement() { return entitlement; }
    public void setEntitlement(Entitlement entitlement) { this.entitlement = entitlement; }
    public Scope getScope() { return scope; }
    public void setScope(Scope scope) { this.scope = scope; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public List<ChangedAttribute> getChangedAttributes() { return changedAttributes; }
    public void setChangedAttributes(List<ChangedAttribute> changedAttributes) { this.changedAttributes = changedAttributes; }
}
