package com.powerbank.rental.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powerbank.rental.dto.kafka.AcquireLockResult;
import com.powerbank.rental.dto.kafka.EjectResult;
import com.powerbank.rental.dto.kafka.PaymentResultEvent;
import com.powerbank.rental.service.RentalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RentalEventConsumer {

    private final ObjectMapper objectMapper;
    private final RentalService rentalService;

    @KafkaListener(topics = "acquire-cabinet-lock-result", groupId = "rental-service")
    public void onLockResult(String message) {
        try {
            AcquireLockResult result = objectMapper.readValue(message, AcquireLockResult.class);
            log.info("Received acquire-cabinet-lock-result: rentalId={} success={}",
                    result.getRentalId(), result.isSuccess());
            rentalService.onLockResult(result);
        } catch (Exception e) {
            log.error("Failed to process acquire-cabinet-lock-result: {}", message, e);
        }
    }

    @KafkaListener(topics = "payment-result", groupId = "rental-service")
    public void onPaymentResult(String message) {
        try {
            PaymentResultEvent result = objectMapper.readValue(message, PaymentResultEvent.class);
            log.info("Received payment-result: rentalId={} status={}",
                    result.getRentalId(), result.getStatus());
            rentalService.onPaymentResult(result);
        } catch (Exception e) {
            log.error("Failed to process payment-result: {}", message, e);
        }
    }

    @KafkaListener(topics = "eject-powerbank-result", groupId = "rental-service")
    public void onEjectResult(String message) {
        try {
            EjectResult result = objectMapper.readValue(message, EjectResult.class);
            log.info("Received eject-powerbank-result: rentalId={} success={}",
                    result.getRentalId(), result.isSuccess());
            rentalService.onEjectResult(result);
        } catch (Exception e) {
            log.error("Failed to process eject-powerbank-result: {}", message, e);
        }
    }
}
