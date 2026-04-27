package com.instantaccessprovider.arcconsolidator.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.instantaccessprovider.arcconsolidator.model.ArcEvent;
import com.instantaccessprovider.arcconsolidator.model.ArcEventType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Mappeur d'événements bruts Active Directory (Windows Security Auditing) vers ArcEvent.
 *
 * <p>EventID pris en charge :
 * <ul>
 *   <li>4720 – Compte créé  : StringInserts [name, domain, sid, actorSid, actorName, actorDomain, logonId, ...]</li>
 *   <li>4722 – Compte activé  (même layout que 4720 pour target/actor)</li>
 *   <li>4725 – Compte désactivé (idem)</li>
 *   <li>4726 – Compte supprimé (idem)</li>
 *   <li>4738 – Compte modifié : StringInserts [samChange, name, domain, sid, actorSid, actorName, actorDomain, logonId, ...]</li>
 * </ul>
 */
@Component
public class ArcEventMapper {

    /** Format de TimeCreated : "2026-04-26 10:43:50 +0200" */
    private static final DateTimeFormatter TIME_CREATED_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT);

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ArcEvent fromRaw(String rawPayload) throws IOException {
        JsonNode node = objectMapper.readTree(rawPayload);

        int eventId         = node.path("EventID").asInt();
        String timeCreated  = node.path("TimeCreated").asText();
        String computer     = node.path("Computer").asText();
        long eventRecordId  = node.path("EventRecordID").asLong();
        JsonNode inserts    = node.path("StringInserts");

        ArcEvent event = new ArcEvent();
        event.setId(computer + "_" + eventRecordId);
        event.setTimestamp(OffsetDateTime.parse(timeCreated, TIME_CREATED_FORMATTER));
        event.setEventType(resolveEventType(eventId));
        event.setSource("activedirectory");
        event.setSourceComputer(computer);
        event.setSourceEventId(eventId);

        switch (eventId) {
            // IDENTITY EVENTS
            case 4720: // Création
            case 4722: // Activation
            case 4725: // Désactivation
            case 4726: // Suppression
                event.setTarget(buildTarget(inserts, 0, 1, 2));
                event.setActor(buildActor(inserts, 3, 4, 5, 6));
                break;
            case 4738: // Modification utilisateur
                event.setTarget(buildTarget(inserts, 1, 2, 3));
                event.setActor(buildActor(inserts, 4, 5, 6, 7));
                // Extraction des attributs modifiés depuis le champ Message
                event.setChangedAttributes(parseChangedAttributes(node.path("Message").asText("")));
                break;
            case 4731: // Modification groupe
                // Extraction des attributs modifiés depuis le champ Message (même logique)
                event.setChangedAttributes(parseChangedAttributes(node.path("Message").asText("")));
                break;
            // Affectation : Ajout/Suppression membre (assignment) — on logge les changements si présents
            case 4728: // Ajout membre groupe
            case 4732: // Ajout membre groupe local
            case 4756: // Ajout membre groupe universel
            case 4729: // Suppression membre groupe
            case 4733: // Suppression membre groupe local
            case 4757: // Suppression membre groupe universel
                // Extraction des changements éventuels (ex: scope, droits, etc.)
                event.setChangedAttributes(parseChangedAttributes(node.path("Message").asText("")));
                break;
            case 4781: // Renommage utilisateur
                // TODO: mapping spécifique pour le renommage
                break;

            // ENTITLEMENT EVENTS (groupes)
            case 4727: // Création groupe
            case 4731: // Modification groupe
            case 4754: // Création groupe universel
            case 4730: // Suppression groupe
            case 4734: // Suppression groupe local
            case 4758: // Suppression groupe universel
                // TODO: mapping Entitlement (groupe)
                break;

            // ASSIGNMENT EVENTS (affectation)
            case 4728: // Ajout membre groupe
            case 4732: // Ajout membre groupe local
            case 4756: // Ajout membre groupe universel
            case 4729: // Suppression membre groupe
            case 4733: // Suppression membre groupe local
            case 4757: // Suppression membre groupe universel
                // TODO: mapping Assignment (affectation, scope optionnel)
                break;

            default:
                if (inserts.size() >= 3) {
                    event.setTarget(buildTarget(inserts, 0, 1, 2));
                }
                break;
        }

        return event;
    }

    /**
     * Parse la section "Changed Attributes" d'un message d'event 4738.
     * Retourne la liste des attributs modifiés (nom, ancienne valeur, nouvelle valeur si possible).
     */
    private java.util.List<ArcEvent.ChangedAttribute> parseChangedAttributes(String message) {
        java.util.List<ArcEvent.ChangedAttribute> changes = new java.util.ArrayList<>();
        if (message == null || !message.contains("Changed Attributes:")) return changes;
        String[] lines = message.split("\r?\n");
        boolean inSection = false;
        for (String line : lines) {
            if (line.trim().startsWith("Changed Attributes:")) {
                inSection = true;
                continue;
            }
            if (inSection) {
                if (line.trim().isEmpty() || line.trim().endsWith(":")) break; // fin de section
                // Format attendu : \tNom attribut:\tValeur
                String[] parts = line.split(":\t", 2);
                if (parts.length == 2) {
                    String name = parts[0].trim().replace("\t", "");
                    String value = parts[1].trim();
                    // Pour les valeurs "Old X"/"New X", on peut améliorer le parsing si besoin
                    changes.add(new ArcEvent.ChangedAttribute(name, null, value));
                }
            }
        }
        return changes;
    }

    private ArcEventType resolveEventType(int eventId) {
        switch (eventId) {
            // IDENTITY
            case 4720: return ArcEventType.USER_CREATED;
            case 4722: return ArcEventType.USER_ENABLED;
            case 4725: return ArcEventType.USER_DISABLED;
            case 4726: return ArcEventType.USER_DELETED;
            case 4738: return ArcEventType.USER_MODIFIED;
            case 4781: return ArcEventType.USER_RENAMED;
            // ENTITLEMENT
            case 4727:
            case 4731:
            case 4754:
                return ArcEventType.GROUP_CREATED;
            case 4730:
            case 4734:
            case 4758:
                return ArcEventType.GROUP_DELETED;
            // ASSIGNMENT
            case 4728:
            case 4732:
            case 4756:
                return ArcEventType.ASSIGNMENT_ADDED;
            case 4729:
            case 4733:
            case 4757:
                return ArcEventType.ASSIGNMENT_REMOVED;
            default:   return ArcEventType.UNKNOWN;
        }
    }

    /** Construit le compte cible à partir des index dans StringInserts. */
    private ArcEvent.Target buildTarget(JsonNode inserts, int nameIdx, int domainIdx, int sidIdx) {
        ArcEvent.Target target = new ArcEvent.Target();
        target.setAccountName(safeGet(inserts, nameIdx));
        target.setDomain(safeGet(inserts, domainIdx));
        target.setSecurityId(safeGet(inserts, sidIdx));
        return target;
    }

    /** Construit le compte acteur à partir des index dans StringInserts. */
    private ArcEvent.Actor buildActor(JsonNode inserts, int sidIdx, int nameIdx, int domainIdx, int logonIdx) {
        ArcEvent.Actor actor = new ArcEvent.Actor();
        actor.setSecurityId(safeGet(inserts, sidIdx));
        actor.setAccountName(safeGet(inserts, nameIdx));
        actor.setDomain(safeGet(inserts, domainIdx));
        actor.setLogonId(safeGet(inserts, logonIdx));
        return actor;
    }

    /** Retourne la valeur de l'index ou null si absent ou égal à "-". */
    private String safeGet(JsonNode array, int index) {
        if (array.isArray() && index < array.size()) {
            String val = array.get(index).asText();
            return "-".equals(val) ? null : val;
        }
        return null;
    }
}
