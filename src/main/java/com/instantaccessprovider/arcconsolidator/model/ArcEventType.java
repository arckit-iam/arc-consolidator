package com.instantaccessprovider.arcconsolidator.model;

/**
 * Types d'événements ARC normalisés correspondant aux événements Windows Security Auditing.
 */
public enum ArcEventType {
    /** EventID 4720 — Un compte utilisateur a été créé. */
    USER_CREATED,
    /** EventID 4722 — Un compte utilisateur a été activé. */
    USER_ENABLED,
    /** EventID 4725 — Un compte utilisateur a été désactivé. */
    USER_DISABLED,
    /** EventID 4726 — Un compte utilisateur a été supprimé. */
    USER_DELETED,
    /** EventID 4738 — Un compte utilisateur a été modifié. */
    USER_MODIFIED,
    /** EventID non géré explicitement. */
    UNKNOWN
}
