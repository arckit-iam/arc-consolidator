// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

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
        dto.setSystem(computer);
        dto.setSourceEventId(eventId);
        dto.setName(safeGet(inserts, 0));        // sAMAccountName (pas de DN disponible pour les events groupe)
        dto.setDisplayName(safeGet(inserts, 0));  // idem
        dto.setExternalId(safeGet(inserts, 2));   // SID
        dto.setType("security-group");
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
        dto.setSystem(computer);
        dto.setSourceEventId(eventId);
        // Layout 4720/4722/4725/4726 : [0]=sAMAccountName, [1]=domain, [2]=SID, ...
        // Layout 4738/4781           : [0]=oldSAMChange("-"), [1]=sAMAccountName, [2]=domain, [3]=SID, ...
        boolean isModified = (eventId == 4738 || eventId == 4781);
        String samAccountName = safeGet(inserts, isModified ? 1 : 0);
        String sid            = safeGet(inserts, isModified ? 3 : 2);
        dto.setName(samAccountName);
        dto.setDisplayName(samAccountName);
        dto.setExternalId(sid);
        dto.setType("user");
        dto.setSystem(computer);
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
        dto.setSystem(computer);
        dto.setSourceEventId(eventId);

        // Identity : [0]=memberDN, [1]=memberDomain, [2]=groupSAMAccountName, [3]=groupDomain, [4]=memberSID, [5]=groupSID
        String memberDn = safeGet(inserts, 0);
        AssignmentEventDTO.Identity identity = new AssignmentEventDTO.Identity();
        identity.setName(memberDn);                             // CN=... / DN du membre
        identity.setDisplayName(extractCN(memberDn));           // nom lisible extrait du DN
        identity.setExternalId(safeGet(inserts, 4));            // SID du membre
        identity.setType("user");
        identity.setSystem(computer);
        dto.setIdentity(identity);

        // Entitlement
        String groupSamAccountName = safeGet(inserts, 2);
        AssignmentEventDTO.Entitlement entitlement = new AssignmentEventDTO.Entitlement();
        entitlement.setName(groupSamAccountName);               // sAMAccountName du groupe (pas de DN dispo)
        entitlement.setDisplayName(groupSamAccountName);        // idem
        entitlement.setExternalId(safeGet(inserts, 5));         // SID du groupe
        entitlement.setType("security-group");
        entitlement.setSystem(computer);
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
                    if ("-".equals(value)) continue; // valeur inchangée, on ignore
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
                    if ("-".equals(value)) continue; // valeur inchangée, on ignore
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
                    if ("-".equals(value)) continue; // valeur inchangée, on ignore
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

    /** Extrait la première valeur CN d'un DN. Ex: "CN=wef-test-0113,CN=Users,DC=iap,DC=local" → "wef-test-0113" */
    private String extractCN(String dn) {
        if (dn == null) return null;
        if (dn.toUpperCase().startsWith("CN=")) {
            int comma = dn.indexOf(',');
            return comma > 3 ? dn.substring(3, comma) : dn.substring(3);
        }
        return dn;
    }
}
