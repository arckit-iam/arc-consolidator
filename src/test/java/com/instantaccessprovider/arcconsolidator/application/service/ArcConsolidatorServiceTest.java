package com.instantaccessprovider.arcconsolidator.application.service;

import com.instantaccessprovider.arcconsolidator.dto.IdentityEventDTO;
import com.instantaccessprovider.arcconsolidator.dto.EntitlementEventDTO;
import com.instantaccessprovider.arcconsolidator.dto.AssignmentEventDTO;
import com.instantaccessprovider.arcconsolidator.mapper.ActiveDirectoryEventMapper;
import com.instantaccessprovider.arcconsolidator.domain.port.ArcEventOutputPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class ArcConsolidatorServiceTest {

    private ActiveDirectoryEventMapper adMapper;

    @Mock
    private ArcEventOutputPort outputPort;

    private ArcConsolidatorService service;

    @BeforeEach
    void setUp() {
        adMapper = spy(new ActiveDirectoryEventMapper());
        service = new ArcConsolidatorService(adMapper, outputPort);
    }

    @Test
    void consolidate_identityEvent_callsPublishIdentity() throws Exception {
        IdentityEventDTO dto = new IdentityEventDTO();
        doReturn(dto).when(adMapper).fromRawToIdentityEventDTO(anyString());
        // EventID 4720 = identity event
        String raw = "{\"EventID\":4720}";
        service.consolidate("activedirectory", raw);
        verify(adMapper).fromRawToIdentityEventDTO(raw);
        verify(outputPort).publishIdentity(dto);
        verifyNoMoreInteractions(outputPort);
    }

    @Test
    void consolidate_entitlementEvent_callsPublishEntitlement() throws Exception {
        EntitlementEventDTO dto = new EntitlementEventDTO();
        doReturn(dto).when(adMapper).fromRawToEntitlementEventDTO(anyString());
        // EventID 4727 = entitlement event
        String raw = "{\"EventID\":4727}";
        service.consolidate("activedirectory", raw);
        verify(adMapper).fromRawToEntitlementEventDTO(raw);
        verify(outputPort).publishEntitlement(dto);
        verifyNoMoreInteractions(outputPort);
    }

    @Test
    void consolidate_assignmentEvent_callsPublishAssignment() throws Exception {
        AssignmentEventDTO dto = new AssignmentEventDTO();
        doReturn(dto).when(adMapper).fromRawToAssignmentEventDTO(anyString());
        // EventID 4728 = assignment event
        String raw = "{\"EventID\":4728}";
        service.consolidate("activedirectory", raw);
        verify(adMapper).fromRawToAssignmentEventDTO(raw);
        verify(outputPort).publishAssignment(dto);
        verifyNoMoreInteractions(outputPort);
    }

    @Test
    void consolidate_unknownEventId_doesNothing() throws Exception {
        // EventID 9999 = inconnu
        String raw = "{\"EventID\":9999}";
        service.consolidate("activedirectory", raw);
        verifyNoInteractions(outputPort);
    }

    @Test
    void consolidate_mapperThrowsException_doesNotPropagateAndSkipsPublish() throws Exception {
        doThrow(new RuntimeException("JSON invalide")).when(adMapper).fromRawToIdentityEventDTO(anyString());
        // EventID 4720 = identity event
        String raw = "{\"EventID\":4720}";
        assertThatCode(() -> service.consolidate("activedirectory", raw)).doesNotThrowAnyException();
        verifyNoInteractions(outputPort);
    }

    @Test
    void consolidate_outputPortThrowsException_doesNotPropagate() throws Exception {
        IdentityEventDTO dto = new IdentityEventDTO();
        doReturn(dto).when(adMapper).fromRawToIdentityEventDTO(anyString());
        doThrow(new RuntimeException("Kafka indisponible")).when(outputPort).publishIdentity(any());
        String raw = "{\"EventID\":4720}";
        assertThatCode(() -> service.consolidate("activedirectory", raw)).doesNotThrowAnyException();
    }
}

