package com.powerbank.station.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StationEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publish(String topic, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, json);
            log.debug("Published to {}: {}", topic, json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payload for topic {}", topic, e);
            throw new RuntimeException("Kafka serialization error", e);
        }
    }

    public void publishLockResult(AcquireLockResult result) {
        publish("acquire-cabinet-lock-result", result);
    }

    public void publishEjectResult(EjectResult result) {
        publish("eject-powerbank-result", result);
    }

    public void publishEjectEvent(String rentalId, String stationId, String slotId) {
        publish("eject-powerbank-event", new EjectEvent(rentalId, stationId, slotId));
    }
}
