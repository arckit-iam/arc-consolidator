package com.instantaccessprovider.arcconsolidator.kafka;

import com.instantaccessprovider.arcconsolidator.model.ArcEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ArcEventPublisher {
    private final KafkaTemplate<String, ArcEvent> kafkaTemplate;
    private final String topic;

    public ArcEventPublisher(KafkaTemplate<String, ArcEvent> kafkaTemplate,
                             @Value("${arc.topic:arc}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(ArcEvent event) {
        kafkaTemplate.send(topic, event.getUser().accountName, event);
    }
}
