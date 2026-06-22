package com.powerbank.rental.service;

import com.powerbank.rental.dto.RentalResponse;
import com.powerbank.rental.dto.kafka.AcquireLockResult;
import com.powerbank.rental.dto.kafka.EjectResult;
import com.powerbank.rental.dto.kafka.PaymentRequestEvent;
import com.powerbank.rental.dto.kafka.PaymentResultEvent;
import com.powerbank.rental.entity.Rental;
import com.powerbank.rental.fsm.RentalFSM;
import com.powerbank.rental.fsm.RentalStatus;
import com.powerbank.rental.kafka.RentalEventProducer;
import com.powerbank.rental.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RentalService {

    private static final BigDecimal RATE_PER_MINUTE = new BigDecimal("100.00");
    private static final BigDecimal MINIMUM_CHARGE  = new BigDecimal("5000.00");

    private final RentalRepository rentalRepository;
    private final RentalFSM fsm;
    private final RentalEventProducer producer;

    @Transactional
    public void initiateRental(Rental rental) {
        fsm.transitionTo(rental, RentalStatus.LOCKING_STATION);
        producer.publishLockEvent(rental.getId().toString(), rental.getStationId());
    }

    @Transactional
    public void onLockResult(AcquireLockResult result) {
        Rental rental = findByIdOrLog(result.getRentalId());
        if (rental == null) return;

        if (result.isSuccess()) {
            rental.setSlotId(result.getSlotId());
            rentalRepository.save(rental);

            fsm.transitionTo(rental, RentalStatus.PROCESSING_PAYMENT);

            producer.publishPaymentRequest(PaymentRequestEvent.builder()
                    .paymentId(UUID.randomUUID().toString())
                    .rentalId(rental.getId().toString())
                    .cardId(rental.getCardId())
                    .userId(rental.getUserId())
                    .amount("5000.00")
                    .idempotencyKey(rental.getIdempotencyKey() + "_initial")
                    .type("INITIAL")
                    .build());
        } else {
            fail(rental, "Station lock failed: " + result.getMessage());
        }
    }

    @Transactional
    public void onPaymentResult(PaymentResultEvent result) {
        Rental rental = findByIdOrLog(result.getRentalId());
        if (rental == null) return;

        if ("SUCCESS".equals(result.getStatus())) {
            fsm.transitionTo(rental, RentalStatus.EJECTING_POWERBANK);
            producer.publishEjectEvent(
                    rental.getId().toString(), rental.getStationId(), rental.getSlotId());
        } else {
            fail(rental, result.getFailureReason() != null
                    ? result.getFailureReason() : "Payment failed");
        }
    }

    @Transactional
    public void onEjectResult(EjectResult result) {
        Rental rental = findByIdOrLog(result.getRentalId());
        if (rental == null) return;

        if (result.isSuccess()) {
            rental.setPowerBankId(result.getPowerBankId());
            rental.setStartedAt(OffsetDateTime.now());
            rentalRepository.save(rental);
            fsm.transitionTo(rental, RentalStatus.IN_THE_LEASE);
        } else {
            fail(rental, "Eject failed: " + result.getMessage());
        }
    }

    @Transactional
    public RentalResponse finishRental(String rentalId, String userId, String stationId) {
        Rental rental = rentalRepository.findById(UUID.fromString(rentalId))
                .orElseThrow(() -> new RuntimeException("Rental not found: " + rentalId));

        if (!rental.getUserId().equals(userId)) {
            throw new RuntimeException("Rental does not belong to user");
        }

        RentalStatus current = RentalStatus.valueOf(rental.getStatus());
        if (current != RentalStatus.IN_THE_LEASE) {
            throw new RuntimeException("Rental is not IN_THE_LEASE, current status: " + current);
        }

        fsm.transitionTo(rental, RentalStatus.FINISHING);

        OffsetDateTime now = OffsetDateTime.now();
        rental.setFinishedAt(now);

        long minutes = ChronoUnit.MINUTES.between(
                rental.getStartedAt() != null ? rental.getStartedAt() : now, now);
        BigDecimal calculated = RATE_PER_MINUTE.multiply(BigDecimal.valueOf(Math.max(minutes, 1)));
        rental.setTotalAmount(calculated.max(MINIMUM_CHARGE));
        rentalRepository.save(rental);

        fsm.transitionTo(rental, RentalStatus.DONE);

        return RentalResponse.builder()
                .rentalId(rental.getId().toString())
                .status(rental.getStatus())
                .totalAmount(rental.getTotalAmount())
                .success(true)
                .message("Rental finished successfully")
                .build();
    }

    public Rental createOrReturnExisting(String userId, String stationId, String cardId,
                                         String idempotencyKey) {
        return rentalRepository.findByIdempotencyKey(idempotencyKey).orElseGet(() -> {
            Rental rental = new Rental();
            rental.setUserId(userId);
            rental.setStationId(stationId);
            rental.setCardId(cardId);
            rental.setIdempotencyKey(idempotencyKey);
            rental.setStatus(RentalStatus.WAITING.name());
            return rentalRepository.save(rental);
        });
    }

    public Rental getById(String rentalId) {
        return rentalRepository.findById(UUID.fromString(rentalId))
                .orElseThrow(() -> new RuntimeException("Rental not found: " + rentalId));
    }

    public List<Rental> getHistory(String userId, int page, int size) {
        return rentalRepository.findAllByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page, size));
    }

    private void fail(Rental rental, String reason) {
        rental.setFailureReason(reason);
        rentalRepository.save(rental);
        fsm.transitionTo(rental, RentalStatus.FAILED);
        log.warn("Rental {} failed: {}", rental.getId(), reason);
    }

    private Rental findByIdOrLog(String rentalId) {
        return rentalRepository.findById(UUID.fromString(rentalId)).orElseGet(() -> {
            log.error("Rental not found for event: {}", rentalId);
            return null;
        });
    }

    public List<RentalResponse> toResponseList(List<Rental> rentals) {
        return rentals.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public RentalResponse toResponse(Rental rental) {
        return RentalResponse.builder()
                .rentalId(rental.getId().toString())
                .status(rental.getStatus())
                .powerBankId(rental.getPowerBankId())
                .slotId(rental.getSlotId())
                .startedAt(rental.getStartedAt())
                .totalAmount(rental.getTotalAmount())
                .build();
    }
}
