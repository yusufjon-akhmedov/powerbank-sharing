package com.powerbank.station.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powerbank.station.service.StationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class StationEventConsumer {

    private final ObjectMapper objectMapper;
    private final StationService stationService;
    private final StationEventProducer producer;

    @KafkaListener(topics = "acquire-cabinet-lock-event", groupId = "station-service")
    public void onAcquireLock(String message) {
        try {
            AcquireLockEvent event = objectMapper.readValue(message, AcquireLockEvent.class);
            log.info("Received acquire-cabinet-lock-event: rentalId={}, stationId={}",
                    event.getRentalId(), event.getStationId());

            try {
                String slotId = stationService.lockSlot(UUID.fromString(event.getStationId()));
                producer.publishLockResult(AcquireLockResult.builder()
                        .rentalId(event.getRentalId())
                        .stationId(event.getStationId())
                        .slotId(slotId)
                        .success(true)
                        .message("Slot locked successfully")
                        .build());
            } catch (Exception e) {
                log.error("Lock failed for rental {}: {}", event.getRentalId(), e.getMessage());
                producer.publishLockResult(AcquireLockResult.builder()
                        .rentalId(event.getRentalId())
                        .stationId(event.getStationId())
                        .success(false)
                        .message(e.getMessage())
                        .build());
            }
        } catch (Exception e) {
            log.error("Failed to process acquire-cabinet-lock-event: {}", message, e);
        }
    }

    @KafkaListener(topics = "eject-powerbank-event", groupId = "station-service")
    public void onEjectPowerBank(String message) {
        try {
            EjectEvent event = objectMapper.readValue(message, EjectEvent.class);
            log.info("Received eject-powerbank-event: rentalId={}, slotId={}",
                    event.getRentalId(), event.getSlotId());

            // Simulate async physical ejection
            Thread.sleep(1000);

            producer.publishEjectResult(EjectResult.builder()
                    .rentalId(event.getRentalId())
                    .powerBankId(event.getSlotId())
                    .success(true)
                    .message("PowerBank ejected successfully")
                    .build());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Eject interrupted for message: {}", message, e);
        } catch (Exception e) {
            log.error("Failed to process eject-powerbank-event: {}", message, e);
        }
    }
}
