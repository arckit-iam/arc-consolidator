// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcconsolidator.mapper;

import com.instantaccessprovider.arcconsolidator.dto.AssignmentEventDTO;
import com.instantaccessprovider.arcconsolidator.dto.EntitlementEventDTO;
import com.instantaccessprovider.arcconsolidator.dto.IdentityEventDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LdapEventMapperTest {

    private static final String SYSTEM = "ldap://LDAP01";
    private static final String ALICE = "uid=alice,ou=people,dc=arc,dc=local";
    private static final String BOB = "uid=bob,ou=people,dc=arc,dc=local";
    private static final String ADMIN = "uid=admin,ou=system";

    private final LdapEventMapper mapper = new LdapEventMapper(ADMIN);

    private static String message(String changeType, String dn, String previousDn, String objectClasses,
                                  String origin, String changes, String entry) {
        return """
                {"id":"LDAP01_uuid-1_2026-11-22T12:00:00Z","source":"ldap","system":"%s","changeType":"%s",
                 "dn":"%s","previousDn":%s,"entryUUID":"uuid-1","objectClasses":%s,
                 "timestamp":"2026-11-22T12:00:00Z","origin":"%s","changes":%s,"entry":%s}
                """.formatted(SYSTEM, changeType, dn, previousDn == null ? "null" : "\"" + previousDn + "\"",
                objectClasses, origin, changes, entry);
    }

    private static final String PERSON = "[\"top\",\"person\",\"inetOrgPerson\"]";
    private static final String GROUP = "[\"top\",\"groupOfNames\"]";

    @Test
    void addPerson_producesCreatedIdentity() throws Exception {
        String raw = message("add", ALICE, null, PERSON, "external", "[]",
                "{\"uid\":[\"alice\"],\"cn\":[\"Alice Martin\"],\"mail\":[\"alice@corp\"]}");

        LdapEventMapper.Result result = mapper.map(raw);

        assertThat(result.entitlements()).isEmpty();
        assertThat(result.assignments()).isEmpty();
        assertThat(result.identities()).hasSize(1);
        IdentityEventDTO dto = result.identities().get(0);
        assertThat(dto.getEventType()).isEqualTo("CREATED");
        assertThat(dto.getName()).isEqualTo("alice");
        assertThat(dto.getDisplayName()).isEqualTo("Alice Martin");
        assertThat(dto.getExternalId()).isEqualTo("uuid-1");
        assertThat(dto.getType()).isEqualTo("user");
        assertThat(dto.getSource()).isEqualTo("ldap");
        assertThat(dto.getSystem()).isEqualTo(SYSTEM);
        assertThat(dto.getOrigin()).isEqualTo("external");
        assertThat(dto.getId()).isEqualTo("LDAP01_uuid-1_2026-11-22T12:00:00Z");
        assertThat(dto.getTimestamp().toInstant()).hasToString("2026-11-22T12:00:00Z");
    }

    @Test
    void modifyPerson_carriesChangedAttributes() throws Exception {
        String raw = message("modify", ALICE, null, PERSON, "arc-agent",
                "[{\"attribute\":\"mail\",\"op\":\"replace\",\"old\":[\"a@corp\"],\"new\":[\"a@live\"]}]",
                "{\"uid\":[\"alice\"],\"cn\":[\"Alice Martin\"],\"mail\":[\"a@live\"]}");

        IdentityEventDTO dto = mapper.map(raw).identities().get(0);

        assertThat(dto.getEventType()).isEqualTo("MODIFIED");
        assertThat(dto.getOrigin()).isEqualTo("arc-agent");
        assertThat(dto.getChangedAttributes()).hasSize(1);
        assertThat(dto.getChangedAttributes().get(0).getName()).isEqualTo("mail");
        assertThat(dto.getChangedAttributes().get(0).getOldValue()).isEqualTo("a@corp");
        assertThat(dto.getChangedAttributes().get(0).getNewValue()).isEqualTo("a@live");
    }

    @Test
    void deletePerson_withoutEntry_usesLastKnownAttributes() throws Exception {
        String raw = message("delete", ALICE, null, PERSON, "external",
                "[{\"attribute\":\"uid\",\"op\":\"delete\",\"old\":[\"alice\"]},"
                        + "{\"attribute\":\"cn\",\"op\":\"delete\",\"old\":[\"Alice Martin\"]}]",
                "null");

        IdentityEventDTO dto = mapper.map(raw).identities().get(0);

        assertThat(dto.getEventType()).isEqualTo("DELETED");
        assertThat(dto.getName()).isEqualTo("alice");
        assertThat(dto.getDisplayName()).isEqualTo("Alice Martin");
    }

    @Test
    void modrdnPerson_isRenamedWithPreviousDn() throws Exception {
        String raw = message("modrdn", "uid=alice2,ou=people,dc=arc,dc=local", ALICE, PERSON, "external", "[]",
                "{\"uid\":[\"alice2\"],\"cn\":[\"Alice Martin\"]}");

        IdentityEventDTO dto = mapper.map(raw).identities().get(0);

        assertThat(dto.getEventType()).isEqualTo("RENAMED");
        assertThat(dto.getChangedAttributes()).hasSize(1);
        assertThat(dto.getChangedAttributes().get(0).getName()).isEqualTo("dn");
        assertThat(dto.getChangedAttributes().get(0).getOldValue()).isEqualTo(ALICE);
    }

    @Test
    void addGroup_producesEntitlementAndOneAssignmentPerMember_ignoringTechnicalMember() throws Exception {
        String raw = message("add", "cn=admins,ou=groups,dc=arc,dc=local", null, GROUP, "arc-agent", "[]",
                "{\"cn\":[\"admins\"],\"member\":[\"" + ADMIN + "\",\"" + ALICE + "\"]}");

        LdapEventMapper.Result result = mapper.map(raw);

        assertThat(result.identities()).isEmpty();
        assertThat(result.entitlements()).hasSize(1);
        EntitlementEventDTO group = result.entitlements().get(0);
        assertThat(group.getEventType()).isEqualTo("CREATED");
        assertThat(group.getName()).isEqualTo("admins");
        assertThat(group.getType()).isEqualTo("group");
        assertThat(group.getExternalId()).isEqualTo("uuid-1");
        assertThat(result.assignments()).hasSize(1);
        AssignmentEventDTO assignment = result.assignments().get(0);
        assertThat(assignment.getEventType()).isEqualTo("ADDED");
        assertThat(assignment.getIdentity().getName()).isEqualTo(ALICE);
        assertThat(assignment.getIdentity().getDisplayName()).isEqualTo("alice");
        assertThat(assignment.getEntitlement().getName()).isEqualTo("admins");
        assertThat(assignment.getOrigin()).isEqualTo("arc-agent");
    }

    @Test
    void modifyMember_producesAddedAndRemovedAssignments_withoutEntitlementEvent() throws Exception {
        String raw = message("modify", "cn=admins,ou=groups,dc=arc,dc=local", null, GROUP, "external",
                "[{\"attribute\":\"member\",\"op\":\"replace\",\"old\":[\"" + ADMIN + "\",\"" + ALICE + "\"],"
                        + "\"new\":[\"" + ADMIN + "\",\"" + BOB + "\"]}]",
                "{\"cn\":[\"admins\"],\"member\":[\"" + ADMIN + "\",\"" + BOB + "\"]}");

        LdapEventMapper.Result result = mapper.map(raw);

        assertThat(result.entitlements()).isEmpty();
        assertThat(result.assignments()).hasSize(2);
        assertThat(result.assignments()).extracting(AssignmentEventDTO::getEventType)
                .containsExactly("ADDED", "REMOVED");
        assertThat(result.assignments().get(0).getIdentity().getName()).isEqualTo(BOB);
        assertThat(result.assignments().get(1).getIdentity().getName()).isEqualTo(ALICE);
        assertThat(result.assignments()).extracting(AssignmentEventDTO::getId).doesNotHaveDuplicates();
    }

    @Test
    void modifyGroup_otherAttribute_producesModifiedEntitlement() throws Exception {
        String raw = message("modify", "cn=admins,ou=groups,dc=arc,dc=local", null, GROUP, "external",
                "[{\"attribute\":\"description\",\"op\":\"add\",\"new\":[\"equipe\"]}]",
                "{\"cn\":[\"admins\"],\"description\":[\"equipe\"]}");

        LdapEventMapper.Result result = mapper.map(raw);

        assertThat(result.entitlements()).hasSize(1);
        assertThat(result.entitlements().get(0).getEventType()).isEqualTo("MODIFIED");
        assertThat(result.assignments()).isEmpty();
    }

    @Test
    void memberComparison_ignoresCaseAndSpaces() throws Exception {
        String raw = message("modify", "cn=admins,ou=groups,dc=arc,dc=local", null, GROUP, "external",
                "[{\"attribute\":\"member\",\"op\":\"replace\",\"old\":[\"UID=alice, ou=people, dc=arc, dc=local\"],"
                        + "\"new\":[\"" + ALICE + "\"]}]",
                "{\"cn\":[\"admins\"]}");

        assertThat(mapper.map(raw).isEmpty()).isTrue();
    }

    @Test
    void deleteGroup_producesDeletedEntitlementOnly() throws Exception {
        String raw = message("delete", "cn=admins,ou=groups,dc=arc,dc=local", null, GROUP, "external",
                "[{\"attribute\":\"cn\",\"op\":\"delete\",\"old\":[\"admins\"]},"
                        + "{\"attribute\":\"member\",\"op\":\"delete\",\"old\":[\"" + ALICE + "\"]}]",
                "null");

        LdapEventMapper.Result result = mapper.map(raw);

        assertThat(result.entitlements()).hasSize(1);
        assertThat(result.entitlements().get(0).getEventType()).isEqualTo("DELETED");
        assertThat(result.entitlements().get(0).getName()).isEqualTo("admins");
        assertThat(result.assignments()).isEmpty();
    }

    @Test
    void otherObjectClass_isIgnored() throws Exception {
        String raw = message("add", "ou=people,dc=arc,dc=local", null, "[\"top\",\"organizationalUnit\"]",
                "external", "[]", "{\"ou\":[\"people\"]}");

        assertThat(mapper.map(raw).isEmpty()).isTrue();
    }

    @Test
    void unknownChangeType_isIgnored() throws Exception {
        String raw = message("bogus", ALICE, null, PERSON, "external", "[]", "{\"uid\":[\"alice\"]}");

        assertThat(mapper.map(raw).isEmpty()).isTrue();
    }

    @Test
    void extractId_returnsMessageId() throws Exception {
        String raw = message("add", ALICE, null, PERSON, "external", "[]", "{\"uid\":[\"alice\"]}");

        assertThat(mapper.extractId(raw)).isEqualTo("LDAP01_uuid-1_2026-11-22T12:00:00Z");
    }
}
