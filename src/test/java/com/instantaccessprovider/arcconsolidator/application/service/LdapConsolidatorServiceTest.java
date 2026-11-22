// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcconsolidator.application.service;

import com.instantaccessprovider.arcconsolidator.domain.port.ArcEventOutputPort;
import com.instantaccessprovider.arcconsolidator.dto.AssignmentEventDTO;
import com.instantaccessprovider.arcconsolidator.dto.EntitlementEventDTO;
import com.instantaccessprovider.arcconsolidator.dto.IdentityEventDTO;
import com.instantaccessprovider.arcconsolidator.mapper.LdapEventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class LdapConsolidatorServiceTest {

    @Mock
    private ArcEventOutputPort outputPort;

    private LdapConsolidatorService service;

    private static String person(String id) {
        return """
                {"id":"%s","system":"ldap://LDAP01","changeType":"add","dn":"uid=alice,ou=people,dc=arc",
                 "entryUUID":"u1","objectClasses":["inetOrgPerson"],"timestamp":"2026-11-22T12:00:00Z",
                 "origin":"external","changes":[],"entry":{"uid":["alice"],"cn":["Alice"]}}
                """.formatted(id);
    }

    @BeforeEach
    void setUp() {
        service = new LdapConsolidatorService(new LdapEventMapper(""), outputPort);
    }

    @Test
    void consolidate_person_publishesIdentity() {
        service.consolidate("ldap", person("m1"));

        verify(outputPort).publishIdentity(any(IdentityEventDTO.class));
        verifyNoMoreInteractions(outputPort);
    }

    @Test
    void consolidate_group_publishesEntitlementAndAssignments() {
        String raw = """
                {"id":"m2","system":"ldap://LDAP01","changeType":"add","dn":"cn=g,ou=groups,dc=arc",
                 "entryUUID":"u2","objectClasses":["groupOfNames"],"timestamp":"2026-11-22T12:00:00Z",
                 "origin":"external","changes":[],
                 "entry":{"cn":["g"],"member":["uid=alice,ou=people,dc=arc","uid=bob,ou=people,dc=arc"]}}
                """;

        service.consolidate("ldap", raw);

        verify(outputPort).publishEntitlement(any(EntitlementEventDTO.class));
        verify(outputPort, times(2)).publishAssignment(any(AssignmentEventDTO.class));
        verifyNoMoreInteractions(outputPort);
    }

    @Test
    void consolidate_sameIdTwice_publishesOnce() {
        service.consolidate("ldap", person("m3"));
        service.consolidate("ldap", person("m3"));

        verify(outputPort, times(1)).publishIdentity(any(IdentityEventDTO.class));
    }

    @Test
    void consolidate_unmappedObjectClass_publishesNothing() {
        String raw = """
                {"id":"m4","changeType":"add","dn":"ou=x","objectClasses":["organizationalUnit"],"entry":{}}
                """;

        service.consolidate("ldap", raw);

        verifyNoInteractions(outputPort);
    }

    @Test
    void consolidate_invalidJson_doesNotThrow() {
        assertThatCode(() -> service.consolidate("ldap", "pas du json")).doesNotThrowAnyException();
        verifyNoInteractions(outputPort);
    }
}
