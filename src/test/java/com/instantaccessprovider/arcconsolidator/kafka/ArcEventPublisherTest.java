package com.instantaccessprovider.arcconsolidator.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.instantaccessprovider.arcconsolidator.model.ArcEvent;
import com.instantaccessprovider.arcconsolidator.model.ArcEventType;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.instantaccessprovider.arcconsolidator.Application;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration de l'adapter Kafka ArcEventPublisher.
 * Vérifie que les événements ARC sont correctement publiés sur le topic Kafka.
 */
@SpringBootTest(classes = Application.class, properties = {"arc.topic=arc-test"})
@EmbeddedKafka(partitions = 1, topics = {"arc-test", "iam.raw.activedirectory"})
@DirtiesContext
class ArcEventPublisherTest {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private ArcEventPublisher publisher;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void publish_sendsArcEventToTopic() throws Exception {
        ArcEvent event = buildEvent("DC01_7399275", ArcEventType.USER_CREATED, "wef-test-5252", "IAP");

        String raw = publishThenPoll(event);

        assertThat(raw).isNotNull();
        ArcEvent received = MAPPER.readValue(raw, ArcEvent.class);
        assertThat(received.getEventType()).isEqualTo(ArcEventType.USER_CREATED);
        assertThat(received.getSource()).isEqualTo("activedirectory");
        assertThat(received.getTarget().getAccountName()).isEqualTo("wef-test-5252");
    }

    @Test
    void publish_nullTargetAccountName_doesNotThrow() {
        ArcEvent event = new ArcEvent();
        event.setId("DC01_0");
        event.setTimestamp(OffsetDateTime.parse("2026-04-26T10:43:50+02:00"));
        event.setEventType(ArcEventType.UNKNOWN);
        event.setSource("activedirectory");

        // Vérifie que la publication ne lève pas d'exception même sans target (clé null)
        org.assertj.core.api.Assertions.assertThatCode(() -> publisher.publish(event))
                .doesNotThrowAnyException();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Attend l'assignation de partition, se positionne à la fin (seekToEnd),
     * publie l'événement, puis consomme uniquement ce message.
     */
    private String publishThenPoll(ArcEvent event) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        try (Consumer<String, String> consumer =
                     new DefaultKafkaConsumerFactory<String, String>(props).createConsumer()) {
            consumer.subscribe(Collections.singletonList("arc-test"));

            // Attendre que l'assignation de partition soit effective
            java.util.Set<org.apache.kafka.common.TopicPartition> assignment =
                    new java.util.HashSet<>();
            long assignDeadline = System.currentTimeMillis() + 5_000;
            while (assignment.isEmpty() && System.currentTimeMillis() < assignDeadline) {
                consumer.poll(Duration.ofMillis(100));
                assignment = consumer.assignment();
            }
            // Se positionner à la fin : on ne lira que les nouveaux messages
            consumer.seekToEnd(assignment);
            // Force la résolution de la position
            assignment.forEach(tp -> consumer.position(tp));

            publisher.publish(event);

            long deadline = System.currentTimeMillis() + 10_000;
            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                if (!records.isEmpty()) {
                    return records.iterator().next().value();
                }
            }
        }
        return null;
    }

    private ArcEvent buildEvent(String id, ArcEventType type, String accountName, String domain) {
        ArcEvent event = new ArcEvent();
        event.setId(id);
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
        actor.setLogonId("0x69f92");
        event.setActor(actor);

        return event;
    }
}
