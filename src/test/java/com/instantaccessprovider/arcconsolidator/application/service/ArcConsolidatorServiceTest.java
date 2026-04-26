package com.instantaccessprovider.arcconsolidator.application.service;

import com.instantaccessprovider.arcconsolidator.domain.port.ArcEventOutputPort;
import com.instantaccessprovider.arcconsolidator.mapper.ArcEventMapper;
import com.instantaccessprovider.arcconsolidator.model.ArcEvent;
import com.instantaccessprovider.arcconsolidator.model.ArcEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du service applicatif ArcConsolidatorService.
 * Le mapper et le port de sortie sont mockés : seul le comportement orchestratoire est vérifié.
 */
@ExtendWith(MockitoExtension.class)
class ArcConsolidatorServiceTest {

    @Mock
    private ArcEventMapper mapper;

    @Mock
    private ArcEventOutputPort outputPort;

    private ArcConsolidatorService service;

    @BeforeEach
    void setUp() {
        service = new ArcConsolidatorService(mapper, outputPort);
    }

    // ── Cas nominal ───────────────────────────────────────────────────────────

    @Test
    void consolidate_callsMapperWithRawPayload() throws IOException {
        ArcEvent event = buildEvent(ArcEventType.USER_CREATED, "alice", "IAP");
        when(mapper.fromRaw(anyString())).thenReturn(event);

        service.consolidate("activedirectory", "{\"EventID\":4720}");

        verify(mapper).fromRaw("{\"EventID\":4720}");
    }

    @Test
    void consolidate_publishesMappedEvent() throws IOException {
        ArcEvent event = buildEvent(ArcEventType.USER_CREATED, "alice", "IAP");
        when(mapper.fromRaw(anyString())).thenReturn(event);

        service.consolidate("activedirectory", "{\"EventID\":4720}");

        ArgumentCaptor<ArcEvent> captor = ArgumentCaptor.forClass(ArcEvent.class);
        verify(outputPort).publish(captor.capture());
        assertThat(captor.getValue()).isSameAs(event);
    }

    @Test
    void consolidate_publishesExactlyOnce() throws IOException {
        when(mapper.fromRaw(anyString())).thenReturn(buildEvent(ArcEventType.USER_DELETED, "bob", "IAP"));

        service.consolidate("activedirectory", "{}");

        verify(outputPort, times(1)).publish(any(ArcEvent.class));
    }

    // ── Gestion des erreurs ───────────────────────────────────────────────────

    @Test
    void consolidate_mapperThrowsIOException_doesNotPropagateAndSkipsPublish() throws IOException {
        when(mapper.fromRaw(anyString())).thenThrow(new IOException("JSON invalide"));

        assertThatCode(() -> service.consolidate("activedirectory", "bad json"))
                .doesNotThrowAnyException();

        verifyNoInteractions(outputPort);
    }

    @Test
    void consolidate_outputPortThrowsException_doesNotPropagate() throws IOException {
        ArcEvent event = buildEvent(ArcEventType.USER_ENABLED, "carol", "IAP");
        when(mapper.fromRaw(anyString())).thenReturn(event);
        doThrow(new RuntimeException("Kafka indisponible")).when(outputPort).publish(any());

        assertThatCode(() -> service.consolidate("activedirectory", "{}"))
                .doesNotThrowAnyException();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ArcEvent buildEvent(ArcEventType type, String accountName, String domain) {
        ArcEvent event = new ArcEvent();
        event.setId("DC01_99999");
        event.setTimestamp(OffsetDateTime.parse("2026-04-26T10:43:50+02:00"));
        event.setEventType(type);
        event.setSource("activedirectory");
        event.setSourceComputer("DC01.iap.local");
        event.setSourceEventId(4720);

        ArcEvent.Target target = new ArcEvent.Target();
        target.setAccountName(accountName);
        target.setDomain(domain);
        target.setSecurityId("S-1-5-21-000-1200");
        event.setTarget(target);

        ArcEvent.Actor actor = new ArcEvent.Actor();
        actor.setAccountName("administrator");
        actor.setDomain(domain);
        actor.setSecurityId("S-1-5-21-000-500");
        actor.setLogonId("0x1234");
        event.setActor(actor);

        return event;
    }
}
