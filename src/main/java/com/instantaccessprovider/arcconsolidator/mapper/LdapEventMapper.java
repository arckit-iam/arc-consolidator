// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcconsolidator.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.instantaccessprovider.arcconsolidator.dto.AssignmentEventDTO;
import com.instantaccessprovider.arcconsolidator.dto.EntitlementEventDTO;
import com.instantaccessprovider.arcconsolidator.dto.IdentityEventDTO;
import com.instantaccessprovider.arcconsolidator.model.ArcEventType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mappeur des changements bruts d'un annuaire LDAP (topic {@code iam.raw.ldap}, produits par
 * {@code arc-agent-openldap}) vers les DTO agnostiques ARC.
 *
 * <ul>
 *   <li>entrée {@code inetOrgPerson} : événement d'identité ;</li>
 *   <li>entrée {@code groupOfNames} : événement de droit, plus un événement d'assignation par valeur
 *       de {@code member} ajoutée ou retirée ;</li>
 *   <li>les autres classes d'objets sont ignorées.</li>
 * </ul>
 *
 * Les membres listés dans {@code iam.ldap.ignored-members} (membre technique d'un groupe) ne produisent
 * aucune assignation.
 */
@Component
public class LdapEventMapper {

    /** Événements ARC produits à partir d'un changement LDAP. */
    public record Result(List<IdentityEventDTO> identities,
                         List<EntitlementEventDTO> entitlements,
                         List<AssignmentEventDTO> assignments) {

        public boolean isEmpty() {
            return identities.isEmpty() && entitlements.isEmpty() && assignments.isEmpty();
        }
    }

    private static final String SOURCE = "ldap";
    private static final String MEMBER = "member";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Set<String> ignoredMembers;

    /**
     * @param ignoredMembers DN des membres techniques à ignorer, séparés par des points-virgules
     *                       (une virgule fait partie d'un DN)
     */
    public LdapEventMapper(@Value("${iam.ldap.ignored-members:}") String ignoredMembers) {
        this.ignoredMembers = java.util.Arrays.stream(ignoredMembers.split(";"))
                .map(LdapEventMapper::normalizeDn)
                .filter(dn -> !dn.isEmpty())
                .collect(Collectors.toSet());
    }

    /** Identifiant du message brut, utilisé pour écarter les republications. */
    public String extractId(String rawPayload) throws IOException {
        return objectMapper.readTree(rawPayload).path("id").asText(null);
    }

    public Result map(String rawPayload) throws IOException {
        JsonNode node = objectMapper.readTree(rawPayload);
        List<IdentityEventDTO> identities = new ArrayList<>();
        List<EntitlementEventDTO> entitlements = new ArrayList<>();
        List<AssignmentEventDTO> assignments = new ArrayList<>();

        ArcEventType eventType = resolveEventType(node.path("changeType").asText());
        Set<String> classes = new HashSet<>();
        node.path("objectClasses").forEach(c -> classes.add(c.asText().toLowerCase(Locale.ROOT)));

        if (eventType != ArcEventType.UNKNOWN) {
            if (classes.contains("inetorgperson") || classes.contains("person")) {
                identities.add(toIdentity(node, eventType));
            } else if (classes.contains("groupofnames") || classes.contains("groupofuniquenames")) {
                boolean memberChanges = hasChange(node, MEMBER);
                if (!(eventType == ArcEventType.MODIFIED && memberChanges && changedAttributes(node, true).isEmpty())) {
                    entitlements.add(toEntitlement(node, eventType));
                }
                assignments.addAll(toAssignments(node, eventType));
            }
        }
        return new Result(identities, entitlements, assignments);
    }

    // ---- Identité --------------------------------------------------------------------------

    private IdentityEventDTO toIdentity(JsonNode node, ArcEventType eventType) {
        String name = firstValue(node, "uid");
        if (name == null) {
            name = rdnValue(node.path("dn").asText(null));
        }
        String cn = firstValue(node, "cn");

        IdentityEventDTO dto = new IdentityEventDTO();
        dto.setId(node.path("id").asText());
        dto.setTimestamp(timestamp(node));
        dto.setEventType(eventType.name());
        dto.setSource(SOURCE);
        dto.setSystem(node.path("system").asText(null));
        dto.setName(name);
        dto.setDisplayName(cn != null ? cn : name);
        dto.setExternalId(node.path("entryUUID").asText(null));
        dto.setType("user");
        dto.setOrigin(node.path("origin").asText(null));
        List<IdentityEventDTO.ChangedAttribute> changed = new ArrayList<>();
        for (String[] c : changedAttributes(node, false)) {
            IdentityEventDTO.ChangedAttribute attr = new IdentityEventDTO.ChangedAttribute();
            attr.setName(c[0]);
            attr.setOldValue(c[1]);
            attr.setNewValue(c[2]);
            changed.add(attr);
        }
        dto.setChangedAttributes(changed);
        return dto;
    }

    // ---- Droit -----------------------------------------------------------------------------

    private EntitlementEventDTO toEntitlement(JsonNode node, ArcEventType eventType) {
        String name = groupName(node);

        EntitlementEventDTO dto = new EntitlementEventDTO();
        dto.setId(node.path("id").asText());
        dto.setTimestamp(timestamp(node));
        dto.setEventType(eventType.name());
        dto.setSource(SOURCE);
        dto.setSystem(node.path("system").asText(null));
        dto.setName(name);
        dto.setDisplayName(name);
        dto.setExternalId(node.path("entryUUID").asText(null));
        dto.setType("group");
        dto.setOrigin(node.path("origin").asText(null));
        List<EntitlementEventDTO.ChangedAttribute> changed = new ArrayList<>();
        for (String[] c : changedAttributes(node, true)) {
            EntitlementEventDTO.ChangedAttribute attr = new EntitlementEventDTO.ChangedAttribute();
            attr.setName(c[0]);
            attr.setOldValue(c[1]);
            attr.setNewValue(c[2]);
            changed.add(attr);
        }
        dto.setChangedAttributes(changed);
        return dto;
    }

    // ---- Assignation -----------------------------------------------------------------------

    private List<AssignmentEventDTO> toAssignments(JsonNode node, ArcEventType eventType) {
        List<String> added = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        if (eventType == ArcEventType.CREATED) {
            node.path("entry").path(MEMBER).forEach(v -> added.add(v.asText()));
        } else if (eventType == ArcEventType.MODIFIED) {
            for (JsonNode change : node.path("changes")) {
                if (!MEMBER.equalsIgnoreCase(change.path("attribute").asText())) {
                    continue;
                }
                Set<String> before = normalizedValues(change.path("old"));
                Set<String> after = normalizedValues(change.path("new"));
                change.path("new").forEach(v -> {
                    if (!before.contains(normalizeDn(v.asText()))) {
                        added.add(v.asText());
                    }
                });
                change.path("old").forEach(v -> {
                    if (!after.contains(normalizeDn(v.asText()))) {
                        removed.add(v.asText());
                    }
                });
            }
        }

        List<AssignmentEventDTO> events = new ArrayList<>();
        int index = 0;
        for (String memberDn : added) {
            index = addAssignment(events, node, ArcEventType.ADDED, memberDn, index);
        }
        for (String memberDn : removed) {
            index = addAssignment(events, node, ArcEventType.REMOVED, memberDn, index);
        }
        return events;
    }

    private int addAssignment(List<AssignmentEventDTO> events, JsonNode node, ArcEventType type,
                              String memberDn, int index) {
        if (ignoredMembers.contains(normalizeDn(memberDn))) {
            return index;
        }
        String system = node.path("system").asText(null);

        AssignmentEventDTO.Identity identity = new AssignmentEventDTO.Identity();
        identity.setName(memberDn);
        identity.setDisplayName(rdnValue(memberDn));
        identity.setType("user");
        identity.setSystem(system);

        String groupName = groupName(node);
        AssignmentEventDTO.Entitlement entitlement = new AssignmentEventDTO.Entitlement();
        entitlement.setName(groupName);
        entitlement.setDisplayName(groupName);
        entitlement.setExternalId(node.path("entryUUID").asText(null));
        entitlement.setType("group");
        entitlement.setSystem(system);

        AssignmentEventDTO dto = new AssignmentEventDTO();
        dto.setId(node.path("id").asText() + "_member_" + type.name().toLowerCase(Locale.ROOT) + "_" + index);
        dto.setTimestamp(timestamp(node));
        dto.setEventType(type.name());
        dto.setSource(SOURCE);
        dto.setSystem(system);
        dto.setIdentity(identity);
        dto.setEntitlement(entitlement);
        dto.setOrigin(node.path("origin").asText(null));
        dto.setChangedAttributes(new ArrayList<>());
        events.add(dto);
        return index + 1;
    }

    // ---- Utilitaires -----------------------------------------------------------------------

    private static ArcEventType resolveEventType(String changeType) {
        return switch (changeType == null ? "" : changeType.toLowerCase(Locale.ROOT)) {
            case "add" -> ArcEventType.CREATED;
            case "modify" -> ArcEventType.MODIFIED;
            case "delete" -> ArcEventType.DELETED;
            case "modrdn" -> ArcEventType.RENAMED;
            default -> ArcEventType.UNKNOWN;
        };
    }

    private static OffsetDateTime timestamp(JsonNode node) {
        String value = node.path("timestamp").asText(null);
        Instant instant = value == null ? Instant.now() : Instant.parse(value);
        return instant.atOffset(ZoneOffset.UTC);
    }

    private static String groupName(JsonNode node) {
        String cn = firstValue(node, "cn");
        return cn != null ? cn : rdnValue(node.path("dn").asText(null));
    }

    private static boolean hasChange(JsonNode node, String attribute) {
        for (JsonNode change : node.path("changes")) {
            if (attribute.equalsIgnoreCase(change.path("attribute").asText())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Première valeur d'un attribut : état de l'entrée, sinon dernière valeur connue d'après les changements
     * (cas d'une suppression, où l'entrée est absente).
     */
    private static String firstValue(JsonNode node, String attribute) {
        var fields = node.path("entry").fields();
        while (fields.hasNext()) {
            var field = fields.next();
            if (field.getKey().equalsIgnoreCase(attribute) && field.getValue().size() > 0) {
                return field.getValue().get(0).asText();
            }
        }
        for (JsonNode change : node.path("changes")) {
            if (attribute.equalsIgnoreCase(change.path("attribute").asText())) {
                JsonNode values = change.path("new").size() > 0 ? change.path("new") : change.path("old");
                if (values.size() > 0) {
                    return values.get(0).asText();
                }
            }
        }
        return null;
    }

    /** Changements sous forme [nom, ancienne valeur, nouvelle valeur] ; les valeurs multiples sont jointes. */
    private static List<String[]> changedAttributes(JsonNode node, boolean excludeMember) {
        List<String[]> result = new ArrayList<>();
        String previousDn = node.path("previousDn").asText(null);
        if (previousDn != null) {
            result.add(new String[]{"dn", previousDn, node.path("dn").asText(null)});
        }
        for (JsonNode change : node.path("changes")) {
            String attribute = change.path("attribute").asText();
            if (excludeMember && MEMBER.equalsIgnoreCase(attribute)) {
                continue;
            }
            result.add(new String[]{attribute, join(change.path("old")), join(change.path("new"))});
        }
        return result;
    }

    private static String join(JsonNode values) {
        if (!values.isArray() || values.isEmpty()) {
            return null;
        }
        List<String> list = new ArrayList<>();
        values.forEach(v -> list.add(v.asText()));
        return String.join(", ", list);
    }

    private static Set<String> normalizedValues(JsonNode values) {
        Set<String> set = new HashSet<>();
        values.forEach(v -> set.add(normalizeDn(v.asText())));
        return set;
    }

    private static String normalizeDn(String dn) {
        return dn == null ? "" : dn.trim().toLowerCase(Locale.ROOT).replaceAll("\\s*,\\s*", ",");
    }

    /** Valeur du premier RDN d'un DN. Ex : "uid=alice,ou=people,dc=arc" donne "alice". */
    static String rdnValue(String dn) {
        if (dn == null) {
            return null;
        }
        int equals = dn.indexOf('=');
        if (equals < 0) {
            return dn;
        }
        int comma = dn.indexOf(',', equals);
        return (comma < 0 ? dn.substring(equals + 1) : dn.substring(equals + 1, comma)).trim();
    }
}
