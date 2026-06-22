package com.powerbank.payment.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powerbank.payment.dto.PaymentRequestEvent;
import com.powerbank.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;

    @KafkaListener(topics = "payment-request", groupId = "payment-service")
    public void onPaymentRequest(String message) {
        try {
            PaymentRequestEvent event = objectMapper.readValue(message, PaymentRequestEvent.class);
            log.info("Received payment-request: rentalId={} idempotencyKey={} amount={}",
                    event.getRentalId(), event.getIdempotencyKey(), event.getAmount());
            paymentService.processPayment(event);
        } catch (Exception e) {
            log.error("Failed to process payment-request message: {}", message, e);
        }
    }
}
