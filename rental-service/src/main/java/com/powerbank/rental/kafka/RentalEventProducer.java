package com.powerbank.rental.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powerbank.rental.dto.kafka.AcquireLockEvent;
import com.powerbank.rental.dto.kafka.EjectEvent;
import com.powerbank.rental.dto.kafka.PaymentRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RentalEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishLockEvent(String rentalId, String stationId) {
        send("acquire-cabinet-lock-event", stationId, new AcquireLockEvent(rentalId, stationId));
    }

    public void publishPaymentRequest(PaymentRequestEvent event) {
        send("payment-request", event.getRentalId(), event);
    }

    public void publishEjectEvent(String rentalId, String stationId, String slotId) {
        send("eject-powerbank-event", stationId, new EjectEvent(rentalId, stationId, slotId));
    }

    private void send(String topic, String key, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, key, json);
            log.debug("Published to {} key={}", topic, key);
        } catch (JsonProcessingException e) {
            log.error("Serialization error for topic {}", topic, e);
            throw new RuntimeException("Kafka serialization error", e);
        }
    }
}
