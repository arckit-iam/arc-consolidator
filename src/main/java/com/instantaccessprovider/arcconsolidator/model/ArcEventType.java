package com.instantaccessprovider.arcconsolidator.model;

/**
 * Types d'événements ARC normalisés correspondant aux événements Windows Security Auditing.
 */
public enum ArcEventType {
    // IDENTITY EVENTS
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
    /** EventID 4781 — Un compte utilisateur a été renommé. */
    USER_RENAMED,

    // ENTITLEMENT EVENTS
    /** EventID 4727, 4731, 4754 — Groupe créé. */
    GROUP_CREATED,
    /** EventID 4731 — Groupe modifié. */
    GROUP_MODIFIED,
    /** EventID 4730, 4734, 4758 — Groupe supprimé. */
    GROUP_DELETED,

    // ASSIGNMENT EVENTS
    /** EventID 4728, 4732, 4756 — Ajout d’un membre à un groupe. */
    ASSIGNMENT_ADDED,
    /** EventID 4729, 4733, 4757 — Suppression d’un membre d’un groupe. */
    ASSIGNMENT_REMOVED,

    /** EventID non géré explicitement. */
    UNKNOWN
}
