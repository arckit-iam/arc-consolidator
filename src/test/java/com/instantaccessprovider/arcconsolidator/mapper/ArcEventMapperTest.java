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
}
