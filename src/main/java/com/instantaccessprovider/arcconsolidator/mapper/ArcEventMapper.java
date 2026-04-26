package com.instantaccessprovider.arcconsolidator.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.instantaccessprovider.arcconsolidator.model.ArcEvent;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * Mappeur d'événements bruts AD (JSON) vers ArcEvent.
 */
@Component
public class ArcEventMapper {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ArcEvent fromRaw(String rawPayload) throws IOException {
        JsonNode node = objectMapper.readTree(rawPayload);
        ArcEvent event = new ArcEvent();
        event.setEventTime(OffsetDateTime.parse(node.path("TimeCreated").asText()));
        event.setEventType("user_created"); // À adapter selon EventID
        event.setSource("activedirectory");
        ArcEvent.User user = new ArcEvent.User();
        user.accountName = node.path("StringInserts").get(0).asText();
        user.domain = node.path("StringInserts").get(1).asText();
        event.setUser(user);
        // ... autres mappings
        return event;
    }
}
