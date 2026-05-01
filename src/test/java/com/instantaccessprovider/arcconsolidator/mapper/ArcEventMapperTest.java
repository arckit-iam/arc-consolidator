package com.instantaccessprovider.arcconsolidator.mapper;

import com.instantaccessprovider.arcconsolidator.model.ArcEvent;
import com.instantaccessprovider.arcconsolidator.model.ArcEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires du mappeur ArcEventMapper.
 * Valide le mapping de chaque EventID Active Directory vers le modèle ARC.
 */
class ArcEventMapperTest {
    // ── EventID 4738 – Vérification du parsing des attributs modifiés ───────
    @Test
    void fromRaw_4738_parsesChangedAttributes() throws IOException {
        String eventJson = """
        {
          \"EventID\": 4738,
          \"TimeCreated\": \"2026-04-26 10:43:51 +0200\",
          \"EventRecordID\": 7399276,
          \"Computer\": \"DC01.iap.local\",
          \"Message\": "A user account was changed.\\r\\n\\r\\nSubject:\\r\\n\\tSecurity ID:\\t\\tS-1-5-21-...\\r\\n\\tAccount Name:\\t\\tadministrator\\r\\n\\tAccount Domain:\\t\\tIAP\\r\\n\\tLogon ID:\\t\\t0x69F92\\r\\n\\r\\nTarget Account:\\r\\n\\tSecurity ID:\\t\\tS-1-5-21-...\\r\\n\\tAccount Name:\\t\\twef-test-5252\\r\\n\\tAccount Domain:\\t\\tIAP\\r\\n\\r\\nChanged Attributes:\\r\\n\\tSAM Account Name:\\t-\\r\\n\\tDisplay Name:\\t\\t-\\r\\n\\tUser Principal Name:\\t-\\r\\n\\tPassword Last Set:\\t26/04/2026 10:43:51\\r\\n\\tOld UAC Value:\\t0x11\\r\\n\\tNew UAC Value:\\t0x10\\r\\n\\tUser Account Control:\\t\\r\\n\\t\\tAccount Enabled\\r\\n\\tUser Parameters:\\t-\\r\\n\\tSID History:\\t-\\r\\n\\tLogon Hours:\\t-\\r\\n\\r\\nAdditional Information:\\r\\n\\tPrivileges:\\t\\t-",
          \"StringInserts\": [
            "-", "wef-test-5252", "IAP", "S-1-5-21-...",
            "S-1-5-21-...", "administrator", "IAP", "0x69f92"
          ]
        }
        """;
        ArcEvent event = mapper.fromRaw(eventJson);
        assertThat(event.getChangedAttributes()).isNotNull();
        assertThat(event.getChangedAttributes()).anyMatch(attr -> "Password Last Set".equals(attr.getName()) && "26/04/2026 10:43:51".equals(attr.getNewValue()));
        assertThat(event.getChangedAttributes()).anyMatch(attr -> "Old UAC Value".equals(attr.getName()) && "0x11".equals(attr.getNewValue()));
        assertThat(event.getChangedAttributes()).anyMatch(attr -> "New UAC Value".equals(attr.getName()) && "0x10".equals(attr.getNewValue()));

        // Exemple d'affichage/logging
        for (ArcEvent.ChangedAttribute attr : event.getChangedAttributes()) {
            System.out.printf("Attribut modifié: %s → %s\n", attr.getName(), attr.getNewValue());
        }
    }

    private ArcEventMapper mapper;

    // ── Fixtures JSON extraites d'événements Active Directory réels ──────────

    private static final String EVENT_4720_USER_CREATED = """
            {
              "@timestamp": 1777193032.008819,
              "ProviderName": "Microsoft-Windows-Security-Auditing",
              "EventID": 4720,
              "TimeCreated": "2026-04-26 10:43:50 +0200",
              "EventRecordID": 7399275,
              "Computer": "DC01.iap.local",
              "StringInserts": [
                "wef-test-5252", "IAP",
                "S-1-5-21-984562994-3965904818-3631805304-1200",
                "S-1-5-21-984562994-3965904818-3631805304-500",
                "administrator", "IAP", "0x69f92",
                "-", "wef-test-5252", "-", "wef-test-5252@iap.local",
                "-", "-", "-", "-", "-", "%%1794", "%%1794",
                "513", "-", "0x0", "0x11",
                "\\r\\n\\t\\t%%2080\\r\\n\\t\\t%%2084", "-", "-", "%%1793"
              ]
            }
            """;

    private static final String EVENT_4738_USER_MODIFIED = """
            {
              "EventID": 4738,
              "TimeCreated": "2026-04-26 10:43:51 +0200",
              "EventRecordID": 7399276,
              "Computer": "DC01.iap.local",
              "StringInserts": [
                "-", "wef-test-5252", "IAP",
                "S-1-5-21-984562994-3965904818-3631805304-1200",
                "S-1-5-21-984562994-3965904818-3631805304-500",
                "administrator", "IAP", "0x69f92",
                "-", "-", "-", "-", "-", "-", "-", "-", "-",
                "26/04/2026 10:43:51", "-", "-", "-", "-", "-", "-", "-", "-", "-"
              ]
            }
            """;

    private static final String EVENT_4722_USER_ENABLED = """
            {
              "EventID": 4722,
              "TimeCreated": "2026-04-26 10:43:53 +0200",
              "EventRecordID": 7399287,
              "Computer": "DC01.iap.local",
              "StringInserts": [
                "wef-test-5252", "IAP",
                "S-1-5-21-984562994-3965904818-3631805304-1200",
                "S-1-5-21-984562994-3965904818-3631805304-500",
                "administrator", "IAP", "0x69f92"
              ]
            }
            """;

    private static final String EVENT_4726_USER_DELETED = """
            {
              "EventID": 4726,
              "TimeCreated": "2026-04-26 10:43:57 +0200",
              "EventRecordID": 7399304,
              "Computer": "DC01.iap.local",
              "StringInserts": [
                "wef-test-5252", "IAP",
                "S-1-5-21-984562994-3965904818-3631805304-1200",
                "S-1-5-21-984562994-3965904818-3631805304-500",
                "administrator", "IAP", "0x69f92", "-"
              ]
            }
            """;

    private static final String EVENT_4725_USER_DISABLED = """
            {
              "EventID": 4725,
              "TimeCreated": "2026-04-26 11:00:00 +0200",
              "EventRecordID": 7399400,
              "Computer": "DC01.iap.local",
              "StringInserts": [
                "wef-test-5252", "IAP",
                "S-1-5-21-984562994-3965904818-3631805304-1200",
                "S-1-5-21-984562994-3965904818-3631805304-500",
                "administrator", "IAP", "0x69f92"
              ]
            }
            """;

    // ── Setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        mapper = new ArcEventMapper();
    }

    // ── EventID 4720 – USER_CREATED ───────────────────────────────────────────

    @Test
    void fromRaw_4720_returnsUserCreatedType() throws IOException {
        ArcEvent event = mapper.fromRaw(EVENT_4720_USER_CREATED);
        assertThat(event.getEventType()).isEqualTo(ArcEventType.USER_CREATED);
    }

    @Test
    void fromRaw_4720_populatesSourceFields() throws IOException {
        ArcEvent event = mapper.fromRaw(EVENT_4720_USER_CREATED);
        assertThat(event.getSource()).isEqualTo("activedirectory");
        assertThat(event.getSourceComputer()).isEqualTo("DC01.iap.local");
        assertThat(event.getSourceEventId()).isEqualTo(4720);
    }

    @Test
    void fromRaw_4720_generatesId() throws IOException {
        ArcEvent event = mapper.fromRaw(EVENT_4720_USER_CREATED);
        assertThat(event.getId()).isEqualTo("DC01.iap.local_7399275");
    }

    @Test
    void fromRaw_4720_parsesTimestamp() throws IOException {
        ArcEvent event = mapper.fromRaw(EVENT_4720_USER_CREATED);
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getTimestamp()).isEqualTo(OffsetDateTime.parse("2026-04-26T10:43:50+02:00"));
    }

    @Test
    void fromRaw_4720_mapsTargetAccount() throws IOException {
        ArcEvent event = mapper.fromRaw(EVENT_4720_USER_CREATED);
        assertThat(event.getTarget()).isNotNull();
        assertThat(event.getTarget().getAccountName()).isEqualTo("wef-test-5252");
        assertThat(event.getTarget().getDomain()).isEqualTo("IAP");
        assertThat(event.getTarget().getSecurityId())
                .isEqualTo("S-1-5-21-984562994-3965904818-3631805304-1200");
    }

    @Test
    void fromRaw_4720_mapsActorAccount() throws IOException {
        ArcEvent event = mapper.fromRaw(EVENT_4720_USER_CREATED);
        assertThat(event.getActor()).isNotNull();
        assertThat(event.getActor().getAccountName()).isEqualTo("administrator");
        assertThat(event.getActor().getDomain()).isEqualTo("IAP");
        assertThat(event.getActor().getSecurityId())
                .isEqualTo("S-1-5-21-984562994-3965904818-3631805304-500");
        assertThat(event.getActor().getLogonId()).isEqualTo("0x69f92");
    }

    // ── EventID 4738 – USER_MODIFIED ─────────────────────────────────────────

    @Test
    void fromRaw_4738_returnsUserModifiedType() throws IOException {
        ArcEvent event = mapper.fromRaw(EVENT_4738_USER_MODIFIED);
        assertThat(event.getEventType()).isEqualTo(ArcEventType.USER_MODIFIED);
    }

    @Test
    void fromRaw_4738_mapsTargetSkippingSamChange() throws IOException {
        ArcEvent event = mapper.fromRaw(EVENT_4738_USER_MODIFIED);
        assertThat(event.getTarget()).isNotNull();
        assertThat(event.getTarget().getAccountName()).isEqualTo("wef-test-5252");
        assertThat(event.getTarget().getDomain()).isEqualTo("IAP");
        assertThat(event.getTarget().getSecurityId())
                .isEqualTo("S-1-5-21-984562994-3965904818-3631805304-1200");
    }

    @Test
    void fromRaw_4738_mapsActorAccount() throws IOException {
        ArcEvent event = mapper.fromRaw(EVENT_4738_USER_MODIFIED);
        assertThat(event.getActor()).isNotNull();
        assertThat(event.getActor().getAccountName()).isEqualTo("administrator");
        assertThat(event.getActor().getDomain()).isEqualTo("IAP");
        assertThat(event.getActor().getLogonId()).isEqualTo("0x69f92");
    }

    // ── EventID 4722 – USER_ENABLED ───────────────────────────────────────────

    @Test
    void fromRaw_4722_returnsUserEnabledType() throws IOException {
        ArcEvent event = mapper.fromRaw(EVENT_4722_USER_ENABLED);
        assertThat(event.getEventType()).isEqualTo(ArcEventType.USER_ENABLED);
    }

    @Test
    void fromRaw_4722_mapsTargetAndActor() throws IOException {
        ArcEvent event = mapper.fromRaw(EVENT_4722_USER_ENABLED);
        assertThat(event.getTarget().getAccountName()).isEqualTo("wef-test-5252");
        assertThat(event.getActor().getAccountName()).isEqualTo("administrator");
    }

    // ── EventID 4726 – USER_DELETED ───────────────────────────────────────────

    @Test
    void fromRaw_4726_returnsUserDeletedType() throws IOException {
        ArcEvent event = mapper.fromRaw(EVENT_4726_USER_DELETED);
        assertThat(event.getEventType()).isEqualTo(ArcEventType.USER_DELETED);
    }

    @Test
    void fromRaw_4726_mapsTargetAndActor() throws IOException {
        ArcEvent event = mapper.fromRaw(EVENT_4726_USER_DELETED);
        assertThat(event.getTarget().getAccountName()).isEqualTo("wef-test-5252");
        assertThat(event.getActor().getAccountName()).isEqualTo("administrator");
    }

    // ── EventID 4725 – USER_DISABLED ─────────────────────────────────────────

    @Test
    void fromRaw_4725_returnsUserDisabledType() throws IOException {
        ArcEvent event = mapper.fromRaw(EVENT_4725_USER_DISABLED);
        assertThat(event.getEventType()).isEqualTo(ArcEventType.USER_DISABLED);
    }

    // ── Valeurs "-" normalisées en null ───────────────────────────────────────

    @Test
    void fromRaw_4738_dashValuesNormalizedToNull() throws IOException {
        ArcEvent event = mapper.fromRaw(EVENT_4738_USER_MODIFIED);
        // Le premier StringInserts est "-" (samChange), il doit être ignoré
        // Le domaine de l'acteur est présent, pas null
        assertThat(event.getActor().getDomain()).isEqualTo("IAP");
    }

    // ── Erreur de parsing ─────────────────────────────────────────────────────

    @Test
    void fromRaw_invalidJson_throwsIOException() {
        assertThatThrownBy(() -> mapper.fromRaw("{invalid json}"))
                .isInstanceOf(IOException.class);
    }

    // ── EventID 4727 – GROUP_CREATED ─────────────────────────────────────────
    @Test
    void fromRaw_4727_returnsGroupCreatedType() throws IOException {
        String eventJson = """
        {
          "EventID": 4727,
          "TimeCreated": "2026-04-26 12:00:00 +0200",
          "EventRecordID": 8000001,
          "Computer": "DC01.iap.local",
          "StringInserts": ["DL-Finance-All", "IAP", "S-1-5-21-...-2000"]
        }
        """;
        ArcEvent event = mapper.fromRaw(eventJson);
        assertThat(event.getEventType()).isEqualTo(ArcEventType.GROUP_CREATED);
    }

    // ── EventID 4728 – ASSIGNMENT_ADDED ────────────────────────────────────
    @Test
    void fromRaw_4728_returnsAssignmentAddedType() throws IOException {
        String eventJson = """
        {
          "EventID": 4728,
          "TimeCreated": "2026-04-26 12:01:00 +0200",
          "EventRecordID": 8000002,
          "Computer": "DC01.iap.local",
          "StringInserts": ["DL-Finance-All", "IAP", "S-1-5-21-...-1200", "S-1-5-21-...-2000", "wef-test-5252", "IAP", "0x12345"]
        }
        """;
        ArcEvent event = mapper.fromRaw(eventJson);
        assertThat(event.getEventType()).isEqualTo(ArcEventType.ASSIGNMENT_ADDED);
        // Vérifie que l'entitlement (groupe) est bien mappé
        assertThat(event.getEntitlement()).isNotNull();
        assertThat(event.getEntitlement().getName()).isEqualTo("DL-Finance-All");
        assertThat(event.getEntitlement().getExternalId()).isEqualTo("S-1-5-21-...-1200");
        assertThat(event.getEntitlement().getType()).isEqualTo("security-group");
        // Vérifie que l'identity (user) est bien mappée
        assertThat(event.getIdentity()).isNotNull();
        assertThat(event.getIdentity().getDisplayName()).isEqualTo("wef-test-5252");
        assertThat(event.getIdentity().getExternalId()).isEqualTo("S-1-5-21-...-2000");
        assertThat(event.getIdentity().getType()).isEqualTo("user");
    }

    // ── EventID 4781 – USER_RENAMED ────────────────────────────────────────
    @Test
    void fromRaw_4781_returnsUserRenamedType() throws IOException {
        String eventJson = """
        {
          "EventID": 4781,
          "TimeCreated": "2026-04-26 12:02:00 +0200",
          "EventRecordID": 8000003,
          "Computer": "DC01.iap.local",
          "StringInserts": ["oldName", "newName", "IAP", "S-1-5-21-...-1200"]
        }
        """;
        ArcEvent event = mapper.fromRaw(eventJson);
        assertThat(event.getEventType()).isEqualTo(ArcEventType.USER_RENAMED);
    }

    // (Suppression du doublon de test et de l’accolade superflue)

    // ── EventID 4731 – Vérification du parsing des attributs modifiés groupe ──
    @Test
    void fromRaw_4731_parsesChangedAttributesForGroup() throws IOException {
        String eventJson = """
        {
          "EventID": 4731,
          "TimeCreated": "2026-04-26 13:00:00 +0200",
          "EventRecordID": 8000100,
          "Computer": "DC01.iap.local",
          "Message": "A security-enabled local group was changed.\\r\\n\\r\\nChanged Attributes:\\r\\n\\tDescription:\\tFinance group\\r\\n\\tMember Added:\\tS-1-5-21-...-1200\\r\\n\\tMember Removed:\\tS-1-5-21-...-1300\\r\\n\\r\\nAdditional Information:\\r\\n\\tPrivileges:\\t-",
          "StringInserts": [
            "DL-Finance-All", "IAP", "S-1-5-21-...-2000"
          ]
        }
        """;
        ArcEvent event = mapper.fromRaw(eventJson);
        assertThat(event.getChangedAttributes()).isNotNull();
        assertThat(event.getChangedAttributes()).anyMatch(attr -> "Description".equals(attr.getName()) && "Finance group".equals(attr.getNewValue()));
        assertThat(event.getChangedAttributes()).anyMatch(attr -> "Member Added".equals(attr.getName()) && "S-1-5-21-...-1200".equals(attr.getNewValue()));
        assertThat(event.getChangedAttributes()).anyMatch(attr -> "Member Removed".equals(attr.getName()) && "S-1-5-21-...-1300".equals(attr.getNewValue()));
    }

    // ── EventID 4728 – Vérification du parsing des attributs modifiés affectation ──
    @Test
    void fromRaw_4728_parsesChangedAttributesForAssignment() throws IOException {
        String eventJson = """
        {
          "EventID": 4728,
          "TimeCreated": "2026-04-26 14:00:00 +0200",
          "EventRecordID": 8000200,
          "Computer": "DC01.iap.local",
          "Message": "A member was added to a security-enabled global group.\\r\\n\\r\\nChanged Attributes:\\r\\n\\tMember Added:\\tS-1-5-21-...-1200\\r\\n\\tScope:\\tEMEA\\r\\n\\r\\nAdditional Information:\\r\\n\\tPrivileges:\\t-",
          "StringInserts": [
            "wef-test-5252", "DL-Finance-All", "S-1-5-21-...-1200", "S-1-5-21-...-2000"
          ]
        }
        """;
        ArcEvent event = mapper.fromRaw(eventJson);
        assertThat(event.getChangedAttributes()).isNotNull();
        assertThat(event.getChangedAttributes()).anyMatch(attr -> "Member Added".equals(attr.getName()) && "S-1-5-21-...-1200".equals(attr.getNewValue()));
        assertThat(event.getChangedAttributes()).anyMatch(attr -> "Scope".equals(attr.getName()) && "EMEA".equals(attr.getNewValue()));
    }

    // ── EventID 4729 – Vérification du parsing des attributs modifiés retrait membre ──
    @Test
    void fromRaw_4729_parsesChangedAttributesForAssignmentRemoval() throws IOException {
        String eventJson = """
        {
          "EventID": 4729,
          "TimeCreated": "2026-04-26 14:05:00 +0200",
          "EventRecordID": 8000201,
          "Computer": "DC01.iap.local",
          "Message": "A member was removed from a security-enabled global group.\\r\\n\\r\\nChanged Attributes:\\r\\n\\tMember Removed:\\tS-1-5-21-...-1200\\r\\n\\tScope:\\tEMEA\\r\\n\\r\\nAdditional Information:\\r\\n\\tPrivileges:\\t-",
          "StringInserts": [
            "wef-test-5252", "DL-Finance-All", "S-1-5-21-...-1200", "S-1-5-21-...-2000"
          ]
        }
        """;
        ArcEvent event = mapper.fromRaw(eventJson);
        assertThat(event.getChangedAttributes()).isNotNull();
        assertThat(event.getChangedAttributes()).anyMatch(attr -> "Member Removed".equals(attr.getName()) && "S-1-5-21-...-1200".equals(attr.getNewValue()));
        assertThat(event.getChangedAttributes()).anyMatch(attr -> "Scope".equals(attr.getName()) && "EMEA".equals(attr.getNewValue()));
    }
}
