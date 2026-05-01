package com.instantaccessprovider.arcconsolidator.model;

/**
 * Types d'événements ARC simplifiés (le type d'objet est porté par le topic Kafka).
 */
public enum ArcEventType {
    CREATED,
    MODIFIED,
    DELETED,
    ENABLED,
    DISABLED,
    RENAMED,
    ADDED,
    REMOVED,
    UNKNOWN
}
