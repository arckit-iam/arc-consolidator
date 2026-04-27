package com.instantaccessprovider.arcconsolidator.model;

/**
 * Modèle Entitlement (groupe, rôle, profil, etc.)
 */
public class Entitlement {
    private String id;
    private String externalId;
    private String sourceSystem;
    private String name;
    private String type;
    private String sensitivityLevel;
    private String owner;
    private java.util.Map<String, Object> metadata;

    // Getters/setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSensitivityLevel() { return sensitivityLevel; }
    public void setSensitivityLevel(String sensitivityLevel) { this.sensitivityLevel = sensitivityLevel; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public java.util.Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(java.util.Map<String, Object> metadata) { this.metadata = metadata; }
}
