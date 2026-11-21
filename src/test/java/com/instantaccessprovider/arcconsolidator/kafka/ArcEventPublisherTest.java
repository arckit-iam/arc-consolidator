// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcconsolidator.kafka;

import com.instantaccessprovider.arcconsolidator.dto.IdentityEventDTO;
import com.instantaccessprovider.arcconsolidator.dto.EntitlementEventDTO;
import com.instantaccessprovider.arcconsolidator.dto.AssignmentEventDTO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import com.instantaccessprovider.arcconsolidator.kafka.publisher.ArcEventPublisher;
import java.util.concurrent.CompletableFuture;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest(classes = com.instantaccessprovider.arcconsolidator.Application.class, properties = {"arc.topic=arc-test"})
class ArcEventPublisherTest {

    @Autowired
    private ArcEventPublisher publisher;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void publishIdentity_sendsIdentityEventDTO() {
        IdentityEventDTO dto = new IdentityEventDTO();
        dto.setId("id-1");
        dto.setDisplayName("alice");
        Mockito.when(kafkaTemplate.send(Mockito.anyString(), Mockito.any(), Mockito.any()))
                .thenReturn(CompletableFuture.completedFuture(Mockito.mock(SendResult.class)));
        assertThatCode(() -> publisher.publishIdentity(dto)).doesNotThrowAnyException();
    }

    @Test
    void publishEntitlement_sendsEntitlementEventDTO() {
        EntitlementEventDTO dto = new EntitlementEventDTO();
        dto.setId("id-2");
        dto.setName("DL-Finance-All");
        Mockito.when(kafkaTemplate.send(Mockito.anyString(), Mockito.any(), Mockito.any()))
                .thenReturn(CompletableFuture.completedFuture(Mockito.mock(SendResult.class)));
        assertThatCode(() -> publisher.publishEntitlement(dto)).doesNotThrowAnyException();
    }

    @Test
    void publishAssignment_sendsAssignmentEventDTO() {
        AssignmentEventDTO dto = new AssignmentEventDTO();
        AssignmentEventDTO.Identity identity = new AssignmentEventDTO.Identity();
        identity.setExternalId("user-123");
        dto.setIdentity(identity);
        dto.setId("id-3");
        Mockito.when(kafkaTemplate.send(Mockito.anyString(), Mockito.any(), Mockito.any()))
                .thenReturn(CompletableFuture.completedFuture(Mockito.mock(SendResult.class)));
        assertThatCode(() -> publisher.publishAssignment(dto)).doesNotThrowAnyException();
    }
}

