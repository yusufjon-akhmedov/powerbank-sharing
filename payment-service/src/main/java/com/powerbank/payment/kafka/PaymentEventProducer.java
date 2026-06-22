package com.powerbank.payment.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powerbank.payment.dto.PaymentResultEvent;
import com.powerbank.payment.dto.PaymentStatusEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishResult(PaymentResultEvent event) {
        send("payment-result", event.getRentalId(), event);
    }

    public void publishStatus(PaymentStatusEvent event) {
        send("payment-events", event.getRentalId(), event);
    }

    private void send(String topic, String key, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, key, json);
            log.debug("Published to {} key={}: {}", topic, key, json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payload for topic {}", topic, e);
            throw new RuntimeException("Kafka serialization error", e);
        }
    }
}
