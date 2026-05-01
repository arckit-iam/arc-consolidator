package com.instantaccessprovider.arcconsolidator.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.instantaccessprovider.arcconsolidator.dto.EntitlementEventDTO;
import com.instantaccessprovider.arcconsolidator.dto.IdentityEventDTO;
import com.instantaccessprovider.arcconsolidator.dto.AssignmentEventDTO;
import com.instantaccessprovider.arcconsolidator.model.ArcEventType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;

/**
 * Mappeur d'événements bruts Active Directory (Windows Security Auditing) vers les DTO agnostiques ARC.
 *
 * Ce mapper est spécifique à la source Active Directory. Pour d'autres sources (ISDS, RACF...),
 * implémenter un autre mapper dédié qui produit les mêmes DTO métier (IdentityEventDTO, EntitlementEventDTO, AssignmentEventDTO).
 *
 * EventID AD pris en charge :
 * <ul>
 *   <li>4720 – Compte créé  : StringInserts [name, domain, sid, actorSid, actorName, actorDomain, logonId, ...]</li>
 *   <li>4722 – Compte activé  (même layout que 4720 pour target/actor)</li>
 *   <li>4725 – Compte désactivé (idem)</li>
 *   <li>4726 – Compte supprimé (idem)</li>
 *   <li>4738 – Compte modifié : StringInserts [samChange, name, domain, sid, actorSid, actorName, actorDomain, logonId, ...]</li>
 * </ul>
 */
@Component
public class ActiveDirectoryEventMapper {
    // Mapping pour EntitlementEventDTO (topic groupe)
    public EntitlementEventDTO fromRawToEntitlementEventDTO(String rawPayload) throws IOException {
        JsonNode node = objectMapper.readTree(rawPayload);
        int eventId = node.path("EventID").asInt();
        String timeCreated = node.path("TimeCreated").asText();
        String computer = node.path("Computer").asText();
        long eventRecordId = node.path("EventRecordID").asLong();
        JsonNode inserts = node.path("StringInserts");

        EntitlementEventDTO dto = new EntitlementEventDTO();
        dto.setId(computer + "_" + eventRecordId);
        dto.setTimestamp(OffsetDateTime.parse(timeCreated, TIME_CREATED_FORMATTER));
        dto.setEventType(resolveEventType(eventId).name());
        dto.setSource("activedirectory");
        dto.setSourceComputer(computer);
        dto.setSourceEventId(eventId);
        dto.setName(safeGet(inserts, 0));
        dto.setExternalId(safeGet(inserts, 2));
        dto.setType("security-group");
        dto.setSourceSystem("activedirectory");
        dto.setChangedAttributes(parseChangedAttributesDTOEntitlement(node.path("Message").asText("")));
        return dto;
    }

    // Mapping pour IdentityEventDTO (topic identité)
    public IdentityEventDTO fromRawToIdentityEventDTO(String rawPayload) throws IOException {
        JsonNode node = objectMapper.readTree(rawPayload);
        int eventId = node.path("EventID").asInt();
        String timeCreated = node.path("TimeCreated").asText();
        String computer = node.path("Computer").asText();
        long eventRecordId = node.path("EventRecordID").asLong();
        JsonNode inserts = node.path("StringInserts");

        IdentityEventDTO dto = new IdentityEventDTO();
        dto.setId(computer + "_" + eventRecordId);
        dto.setTimestamp(OffsetDateTime.parse(timeCreated, TIME_CREATED_FORMATTER));
        dto.setEventType(resolveEventType(eventId).name());
        dto.setSource("activedirectory");
        dto.setSourceComputer(computer);
        dto.setSourceEventId(eventId);
        dto.setDisplayName(safeGet(inserts, 0));
        dto.setExternalId(safeGet(inserts, 2));
        dto.setType("user");
        dto.setSourceSystem("activedirectory");
        dto.setChangedAttributes(parseChangedAttributesDTOIdentity(node.path("Message").asText("")));
        return dto;
    }

    // Mapping pour AssignmentEventDTO (topic assignation)
    public AssignmentEventDTO fromRawToAssignmentEventDTO(String rawPayload) throws IOException {
        JsonNode node = objectMapper.readTree(rawPayload);
        int eventId = node.path("EventID").asInt();
        String timeCreated = node.path("TimeCreated").asText();
        String computer = node.path("Computer").asText();
        long eventRecordId = node.path("EventRecordID").asLong();
        JsonNode inserts = node.path("StringInserts");

        AssignmentEventDTO dto = new AssignmentEventDTO();
        dto.setId(computer + "_" + eventRecordId);
        dto.setTimestamp(OffsetDateTime.parse(timeCreated, TIME_CREATED_FORMATTER));
        dto.setEventType(resolveEventType(eventId).name());
        dto.setSource("activedirectory");
        dto.setSourceComputer(computer);
        dto.setSourceEventId(eventId);

        // Identity
        AssignmentEventDTO.Identity identity = new AssignmentEventDTO.Identity();
        identity.setDisplayName(safeGet(inserts, 4));
        identity.setExternalId(safeGet(inserts, 6));
        identity.setType("user");
        identity.setSourceSystem("activedirectory");
        dto.setIdentity(identity);

        // Entitlement
        AssignmentEventDTO.Entitlement entitlement = new AssignmentEventDTO.Entitlement();
        entitlement.setName(safeGet(inserts, 0));
        entitlement.setExternalId(safeGet(inserts, 2));
        entitlement.setType("security-group");
        entitlement.setSourceSystem("activedirectory");
        dto.setEntitlement(entitlement);

        dto.setChangedAttributes(parseChangedAttributesDTOAssignment(node.path("Message").asText("")));
        return dto;
    }

    // Conversion ChangedAttribute (ArcEvent) -> ChangedAttribute (DTO) pour IdentityEventDTO
    private List<IdentityEventDTO.ChangedAttribute> parseChangedAttributesDTOIdentity(String message) {
        List<IdentityEventDTO.ChangedAttribute> changes = new ArrayList<>();
        if (message == null || !message.contains("Changed Attributes:")) return changes;
        String[] lines = message.split("\r?\n");
        boolean inSection = false;
        for (String line : lines) {
            if (line.trim().startsWith("Changed Attributes:")) {
                inSection = true;
                continue;
            }
            if (inSection) {
                if (line.trim().isEmpty() || line.trim().endsWith(":")) break;
                String[] parts = line.split(":\t", 2);
                if (parts.length == 2) {
                    String name = parts[0].trim().replace("\t", "");
                    String value = parts[1].trim();
                    IdentityEventDTO.ChangedAttribute attr = new IdentityEventDTO.ChangedAttribute();
                    attr.setName(name);
                    attr.setNewValue(value);
                    changes.add(attr);
                }
            }
        }
        return changes;
    }

    // Conversion ChangedAttribute (ArcEvent) -> ChangedAttribute (DTO) pour EntitlementEventDTO
    private List<EntitlementEventDTO.ChangedAttribute> parseChangedAttributesDTOEntitlement(String message) {
        List<EntitlementEventDTO.ChangedAttribute> changes = new ArrayList<>();
        if (message == null || !message.contains("Changed Attributes:")) return changes;
        String[] lines = message.split("\r?\n");
        boolean inSection = false;
        for (String line : lines) {
            if (line.trim().startsWith("Changed Attributes:")) {
                inSection = true;
                continue;
            }
            if (inSection) {
                if (line.trim().isEmpty() || line.trim().endsWith(":")) break;
                String[] parts = line.split(":\t", 2);
                if (parts.length == 2) {
                    String name = parts[0].trim().replace("\t", "");
                    String value = parts[1].trim();
                    EntitlementEventDTO.ChangedAttribute attr = new EntitlementEventDTO.ChangedAttribute();
                    attr.setName(name);
                    attr.setNewValue(value);
                    changes.add(attr);
                }
            }
        }
        return changes;
    }

    // Conversion ChangedAttribute (ArcEvent) -> ChangedAttribute (DTO) pour AssignmentEventDTO
    private List<AssignmentEventDTO.ChangedAttribute> parseChangedAttributesDTOAssignment(String message) {
        List<AssignmentEventDTO.ChangedAttribute> changes = new ArrayList<>();
        if (message == null || !message.contains("Changed Attributes:")) return changes;
        String[] lines = message.split("\r?\n");
        boolean inSection = false;
        for (String line : lines) {
            if (line.trim().startsWith("Changed Attributes:")) {
                inSection = true;
                continue;
            }
            if (inSection) {
                if (line.trim().isEmpty() || line.trim().endsWith(":")) break;
                String[] parts = line.split(":\t", 2);
                if (parts.length == 2) {
                    String name = parts[0].trim().replace("\t", "");
                    String value = parts[1].trim();
                    AssignmentEventDTO.ChangedAttribute attr = new AssignmentEventDTO.ChangedAttribute();
                    attr.setName(name);
                    attr.setNewValue(value);
                    changes.add(attr);
                }
            }
        }
        return changes;
    }

    /** Format de TimeCreated : "2026-04-26 10:43:50 +0200" */
    private static final DateTimeFormatter TIME_CREATED_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT);

    public final ObjectMapper objectMapper = new ObjectMapper();

    private ArcEventType resolveEventType(int eventId) {
        switch (eventId) {
            // IDENTITY
            case 4720: return ArcEventType.CREATED;
            case 4722: return ArcEventType.ENABLED;
            case 4725: return ArcEventType.DISABLED;
            case 4726: return ArcEventType.DELETED;
            case 4738: return ArcEventType.MODIFIED;
            case 4781: return ArcEventType.RENAMED;
            // ENTITLEMENT
            case 4727:
            case 4754:
                return ArcEventType.CREATED;
            case 4731:
                return ArcEventType.MODIFIED;
            case 4730:
            case 4734:
            case 4758:
                return ArcEventType.DELETED;
            // ASSIGNMENT
            case 4728:
            case 4732:
            case 4756:
                return ArcEventType.ADDED;
            case 4729:
            case 4733:
            case 4757:
                return ArcEventType.REMOVED;
            default:   return ArcEventType.UNKNOWN;
        }
    }

    private String safeGet(JsonNode array, int index) {
        if (array.isArray() && index < array.size()) {
            String val = array.get(index).asText();
            return "-".equals(val) ? null : val;
        }
        return null;
    }
}
