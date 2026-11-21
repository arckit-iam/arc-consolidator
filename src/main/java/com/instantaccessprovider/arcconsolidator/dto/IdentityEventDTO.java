// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcconsolidator.dto;

import java.time.OffsetDateTime;
import java.util.List;

public class IdentityEventDTO {
    private String id;
    private OffsetDateTime timestamp;
    private String eventType;
    private String source;
    private String system;
    private int sourceEventId;
    private String name;        // CN=... / DN
    private String displayName; // nom lisible (sAMAccountName)
    private String externalId;  // SID
    private String type;
    private List<ChangedAttribute> changedAttributes;

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
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public List<ChangedAttribute> getChangedAttributes() { return changedAttributes; }
    public void setChangedAttributes(List<ChangedAttribute> changedAttributes) { this.changedAttributes = changedAttributes; }
}
