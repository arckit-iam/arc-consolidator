package com.instantaccessprovider.arcconsolidator.mapper;

import com.instantaccessprovider.arcconsolidator.dto.IdentityEventDTO;
import com.instantaccessprovider.arcconsolidator.dto.EntitlementEventDTO;
import com.instantaccessprovider.arcconsolidator.dto.AssignmentEventDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires du mappeur ActiveDirectoryEventMapper.
 * Valide le mapping de chaque EventID Active Directory vers les DTOs métier.
 */
class ActiveDirectoryEventMapperTest {
    private ActiveDirectoryEventMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ActiveDirectoryEventMapper();
    }

    // ── EventID 4720 – USER_CREATED (Identity) ──────────────────────────────
    @Test
    void fromRawToIdentityEventDTO_4720_mapsFields() throws IOException {
        String eventJson = """
        {
          "EventID": 4720,
          "TimeCreated": "2026-04-26 10:43:50 +0200",
          "EventRecordID": 7399275,
          "Computer": "DC01.iap.local",
          "StringInserts": [
            "wef-test-5252", "IAP",
            "S-1-5-21-984562994-3965904818-3631805304-1200",
            "S-1-5-21-984562994-3965904818-3631805304-500",
            "administrator", "IAP", "0x69f92"
          ]
        }
        """;
        IdentityEventDTO dto = mapper.fromRawToIdentityEventDTO(eventJson);
        assertThat(dto).isNotNull();
        assertThat(dto.getName()).isEqualTo("wef-test-5252");
        assertThat(dto.getDisplayName()).isEqualTo("wef-test-5252");
        assertThat(dto.getSource()).isEqualTo("activedirectory");
        assertThat(dto.getId()).isEqualTo("DC01.iap.local_7399275");
        assertThat(dto.getTimestamp()).isEqualTo(OffsetDateTime.parse("2026-04-26T10:43:50+02:00"));
        assertThat(dto.getEventType()).isEqualTo("CREATED");
    }

    // ── EventID 4738 – USER_MODIFIED (Identity) ─────────────────────────────
    @Test
    void fromRawToIdentityEventDTO_4738_mapsChangedAttributes() throws IOException {
        String eventJson = """
        {
          "EventID": 4738,
          "TimeCreated": "2026-04-26 10:43:51 +0200",
          "EventRecordID": 7399276,
          "Computer": "DC01.iap.local",
          "Message": "A user account was changed.\\r\\n\\r\\nChanged Attributes:\\r\\n\\tPassword Last Set:\\t26/04/2026 10:43:51\\r\\n\\tOld UAC Value:\\t0x11\\r\\n\\tNew UAC Value:\\t0x10",
          "StringInserts": [
            "-", "wef-test-5252", "IAP", "S-1-5-21-...",
            "S-1-5-21-...", "administrator", "IAP", "0x69f92"
          ]
        }
        """;
        IdentityEventDTO dto = mapper.fromRawToIdentityEventDTO(eventJson);
        // inserts[1]=sAMAccountName, inserts[3]=SID pour les events MODIFIED (4738)
        assertThat(dto.getName()).isEqualTo("wef-test-5252");
        assertThat(dto.getDisplayName()).isEqualTo("wef-test-5252");
        assertThat(dto.getExternalId()).isEqualTo("S-1-5-21-...");
        List<IdentityEventDTO.ChangedAttribute> attrs = dto.getChangedAttributes();
        assertThat(attrs).isNotNull();
        assertThat(attrs).anyMatch(attr -> "Password Last Set".equals(attr.getName()) && "26/04/2026 10:43:51".equals(attr.getNewValue()));
        assertThat(attrs).anyMatch(attr -> "Old UAC Value".equals(attr.getName()) && "0x11".equals(attr.getNewValue()));
        assertThat(attrs).anyMatch(attr -> "New UAC Value".equals(attr.getName()) && "0x10".equals(attr.getNewValue()));
    }

    // ── EventID 4727 – GROUP_CREATED (Entitlement) ──────────────────────────
    @Test
    void fromRawToEntitlementEventDTO_4727_mapsFields() throws IOException {
        String eventJson = """
        {
          "EventID": 4727,
          "TimeCreated": "2026-04-26 12:00:00 +0200",
          "EventRecordID": 8000001,
          "Computer": "DC01.iap.local",
          "StringInserts": ["DL-Finance-All", "IAP", "S-1-5-21-...-2000"]
        }
        """;
        EntitlementEventDTO dto = mapper.fromRawToEntitlementEventDTO(eventJson);
        assertThat(dto).isNotNull();
        assertThat(dto.getName()).isEqualTo("DL-Finance-All");
        assertThat(dto.getDisplayName()).isEqualTo("DL-Finance-All");
        assertThat(dto.getExternalId()).isEqualTo("S-1-5-21-...-2000");
        assertThat(dto.getType()).isEqualTo("security-group");
        assertThat(dto.getEventType()).isEqualTo("CREATED");
    }

    // ── EventID 4731 – GROUP_MODIFIED (Entitlement) ─────────────────────────
    @Test
    void fromRawToEntitlementEventDTO_4731_mapsChangedAttributes() throws IOException {
        String eventJson = """
        {
          "EventID": 4731,
          "TimeCreated": "2026-04-26 13:00:00 +0200",
          "EventRecordID": 8000100,
          "Computer": "DC01.iap.local",
          "Message": "A security-enabled local group was changed.\\r\\n\\r\\nChanged Attributes:\\r\\n\\tDescription:\\tFinance group\\r\\n\\tMember Added:\\tS-1-5-21-...-1200\\r\\n\\tMember Removed:\\tS-1-5-21-...-1300",
          "StringInserts": ["DL-Finance-All", "IAP", "S-1-5-21-...-2000"]
        }
        """;
        EntitlementEventDTO dto = mapper.fromRawToEntitlementEventDTO(eventJson);
        List<EntitlementEventDTO.ChangedAttribute> attrs = dto.getChangedAttributes();
        assertThat(attrs).isNotNull();
        assertThat(attrs).anyMatch(attr -> "Description".equals(attr.getName()) && "Finance group".equals(attr.getNewValue()));
        assertThat(attrs).anyMatch(attr -> "Member Added".equals(attr.getName()) && "S-1-5-21-...-1200".equals(attr.getNewValue()));
        assertThat(attrs).anyMatch(attr -> "Member Removed".equals(attr.getName()) && "S-1-5-21-...-1300".equals(attr.getNewValue()));
    }

    // ── EventID 4728 – ASSIGNMENT_ADDED (Assignment) ────────────────────────
    @Test
    void fromRawToAssignmentEventDTO_4728_mapsFields() throws IOException {
        String eventJson = """
        {
          "EventID": 4728,
          "TimeCreated": "2026-04-26 12:01:00 +0200",
          "EventRecordID": 8000002,
          "Computer": "DC01.iap.local",
          "StringInserts": ["CN=wef-test-5252,CN=Users,DC=iap,DC=local", "IAP", "DL-Finance-All", "IAP", "S-1-5-21-...-1200", "S-1-5-21-...-2000", "0x12345"]
        }
        """;
        AssignmentEventDTO dto = mapper.fromRawToAssignmentEventDTO(eventJson);
        assertThat(dto).isNotNull();
        assertThat(dto.getIdentity()).isNotNull();
        assertThat(dto.getIdentity().getName()).isEqualTo("CN=wef-test-5252,CN=Users,DC=iap,DC=local");
        assertThat(dto.getIdentity().getDisplayName()).isEqualTo("wef-test-5252");
        assertThat(dto.getIdentity().getExternalId()).isEqualTo("S-1-5-21-...-1200");
        assertThat(dto.getIdentity().getType()).isEqualTo("user");
        assertThat(dto.getEntitlement()).isNotNull();
        assertThat(dto.getEntitlement().getName()).isEqualTo("DL-Finance-All");
        assertThat(dto.getEntitlement().getDisplayName()).isEqualTo("DL-Finance-All");
        assertThat(dto.getEntitlement().getExternalId()).isEqualTo("S-1-5-21-...-2000");
        assertThat(dto.getEntitlement().getType()).isEqualTo("security-group");
    }

    // ── EventID 4728 – ASSIGNMENT_ADDED (Assignment) – ChangedAttributes ────
    @Test
    void fromRawToAssignmentEventDTO_4728_mapsChangedAttributes() throws IOException {
        String eventJson = """
        {
          "EventID": 4728,
          "TimeCreated": "2026-04-26 14:00:00 +0200",
          "EventRecordID": 8000200,
          "Computer": "DC01.iap.local",
          "Message": "A member was added to a security-enabled global group.\\r\\n\\r\\nChanged Attributes:\\r\\n\\tMember Added:\\tS-1-5-21-...-1200\\r\\n\\tScope:\\tEMEA",
          "StringInserts": ["wef-test-5252", "DL-Finance-All", "S-1-5-21-...-1200", "S-1-5-21-...-2000"]
        }
        """;
        AssignmentEventDTO dto = mapper.fromRawToAssignmentEventDTO(eventJson);
        List<AssignmentEventDTO.ChangedAttribute> attrs = dto.getChangedAttributes();
        assertThat(attrs).isNotNull();
        assertThat(attrs).anyMatch(attr -> "Member Added".equals(attr.getName()) && "S-1-5-21-...-1200".equals(attr.getNewValue()));
        assertThat(attrs).anyMatch(attr -> "Scope".equals(attr.getName()) && "EMEA".equals(attr.getNewValue()));
    }

    // ── EventID 4729 – ASSIGNMENT_REMOVED (Assignment) – ChangedAttributes ──
    @Test
    void fromRawToAssignmentEventDTO_4729_mapsChangedAttributes() throws IOException {
        String eventJson = """
        {
          "EventID": 4729,
          "TimeCreated": "2026-04-26 14:05:00 +0200",
          "EventRecordID": 8000201,
          "Computer": "DC01.iap.local",
          "Message": "A member was removed from a security-enabled global group.\\r\\n\\r\\nChanged Attributes:\\r\\n\\tMember Removed:\\tS-1-5-21-...-1200\\r\\n\\tScope:\\tEMEA",
          "StringInserts": ["wef-test-5252", "DL-Finance-All", "S-1-5-21-...-1200", "S-1-5-21-...-2000"]
        }
        """;
        AssignmentEventDTO dto = mapper.fromRawToAssignmentEventDTO(eventJson);
        List<AssignmentEventDTO.ChangedAttribute> attrs = dto.getChangedAttributes();
        assertThat(attrs).isNotNull();
        assertThat(attrs).anyMatch(attr -> "Member Removed".equals(attr.getName()) && "S-1-5-21-...-1200".equals(attr.getNewValue()));
        assertThat(attrs).anyMatch(attr -> "Scope".equals(attr.getName()) && "EMEA".equals(attr.getNewValue()));
    }

    // ── 4738 : name depuis inserts[1] ; valeurs "-" filtrées dans changedAttributes ─────
    @Test
    void fromRawToIdentityEventDTO_dashValuesNormalizedToNull() throws IOException {
        String eventJson = """
        {
          "EventID": 4738,
          "TimeCreated": "2026-04-26 10:43:51 +0200",
          "EventRecordID": 7399276,
          "Computer": "DC01.iap.local",
          "Message": "Changed Attributes: \\r\\n\\tDescription:\\t-\\r\\n\\tOld UAC Value:\\t0x10",
          "StringInserts": [
            "-", "wef-test-5252", "IAP", "S-1-5-21-984562994-3965904818-3631805304-1200",
            "S-1-5-21-984562994-3965904818-3631805304-500", "administrator", "IAP", "0x69f92"
          ]
        }
        """;
        IdentityEventDTO dto = mapper.fromRawToIdentityEventDTO(eventJson);
        // 4738 : name/displayName viennent de inserts[1], pas inserts[0] ("-")
        assertThat(dto.getName()).isEqualTo("wef-test-5252");
        assertThat(dto.getDisplayName()).isEqualTo("wef-test-5252");
        assertThat(dto.getType()).isEqualTo("user");
        // Les attributs avec valeur "-" sont filtrés
        List<IdentityEventDTO.ChangedAttribute> attrs = dto.getChangedAttributes();
        assertThat(attrs).noneMatch(attr -> "-".equals(attr.getNewValue()));
        assertThat(attrs).anyMatch(attr -> "Old UAC Value".equals(attr.getName()) && "0x10".equals(attr.getNewValue()));
    }

    // ── Erreur de parsing ───────────────────────────────────────────────────
    @Test
    void fromRawToIdentityEventDTO_invalidJson_throwsIOException() {
      assertThatThrownBy(() -> mapper.fromRawToIdentityEventDTO("{invalid json}"))
          .isInstanceOf(IOException.class);
    }
  }
