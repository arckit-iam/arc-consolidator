package com.instantaccessprovider.arcconsolidator.kafka;

import com.instantaccessprovider.arcconsolidator.mapper.ArcEventMapper;
import com.instantaccessprovider.arcconsolidator.model.ArcEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RawIamEventConsumer {
    private final ArcEventMapper mapper;
    private final ArcEventPublisher publisher;

    public RawIamEventConsumer(ArcEventMapper mapper, ArcEventPublisher publisher) {
        this.mapper = mapper;
        this.publisher = publisher;
    }

    @KafkaListener(topics = "iam.raw.activedirectory", groupId = "arc-consolidator")
    public void onMessage(ConsumerRecord<String, String> record) {
        try {
            ArcEvent arcEvent = mapper.fromRaw(record.value());
            publisher.publish(arcEvent);
        } catch (Exception e) {
            // TODO: log/handle error
        }
    }
}
