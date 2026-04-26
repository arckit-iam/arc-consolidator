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
            case 4720: // Création  — [name, domain, sid,  actorSid, actorName, actorDomain, logonId]
            case 4722: // Activation — même layout
            case 4725: // Désactivation — même layout
            case 4726: // Suppression — même layout
                event.setTarget(buildTarget(inserts, 0, 1, 2));
                event.setActor(buildActor(inserts, 3, 4, 5, 6));
                break;
            case 4738: // Modification — [samChange, name, domain, sid, actorSid, actorName, actorDomain, logonId]
                event.setTarget(buildTarget(inserts, 1, 2, 3));
                event.setActor(buildActor(inserts, 4, 5, 6, 7));
                break;
            default:
                if (inserts.size() >= 3) {
                    event.setTarget(buildTarget(inserts, 0, 1, 2));
                }
                break;
        }

        return event;
    }

    private ArcEventType resolveEventType(int eventId) {
        switch (eventId) {
            case 4720: return ArcEventType.USER_CREATED;
            case 4722: return ArcEventType.USER_ENABLED;
            case 4725: return ArcEventType.USER_DISABLED;
            case 4726: return ArcEventType.USER_DELETED;
            case 4738: return ArcEventType.USER_MODIFIED;
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
