package com.powerbank.payment.service;

import com.powerbank.payment.dto.PaymentRequestEvent;
import com.powerbank.payment.dto.PaymentResultEvent;
import com.powerbank.payment.dto.PaymentStatusEvent;
import com.powerbank.payment.entity.Card;
import com.powerbank.payment.entity.Payment;
import com.powerbank.payment.kafka.PaymentEventProducer;
import com.powerbank.payment.repository.CardRepository;
import com.powerbank.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CardRepository cardRepository;
    private final PaymentEventProducer producer;

    @Transactional
    public void processPayment(PaymentRequestEvent request) {
        BigDecimal amount = new BigDecimal(request.getAmount());

        // 1. Idempotency check
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            Payment payment = existing.get();
            if (payment.getAmount().compareTo(amount) != 0) {
                log.warn("Idempotency conflict: same key={}, different amount existing={} new={}",
                        request.getIdempotencyKey(), payment.getAmount(), amount);
            }
            if ("SUCCESS".equals(payment.getStatus())) {
                log.info("Duplicate payment request for idempotencyKey={}, returning existing result",
                        request.getIdempotencyKey());
                publishResult(payment);
                return;
            }
            if ("PENDING".equals(payment.getStatus())) {
                log.info("Payment already in PENDING state for idempotencyKey={}", request.getIdempotencyKey());
                return;
            }
        }

        // 2. Create new payment in PENDING state
        Payment payment = new Payment();
        payment.setRentalId(request.getRentalId());
        payment.setCardId(request.getCardId() != null ? UUID.fromString(request.getCardId()) : null);
        payment.setUserId(request.getUserId());
        payment.setAmount(amount);
        payment.setIdempotencyKey(request.getIdempotencyKey());
        payment.setType(request.getType());
        payment.setStatus("PENDING");
        paymentRepository.save(payment);

        publishStatus(payment);

        // 3. Find card
        if (request.getCardId() == null) {
            failPayment(payment, "Card ID not provided");
            return;
        }

        Optional<Card> cardOpt = cardRepository.findById(UUID.fromString(request.getCardId()));
        if (cardOpt.isEmpty()) {
            failPayment(payment, "Card not found");
            return;
        }

        Card card = cardOpt.get();

        // 4. Validate card
        if ("BLOCKED".equals(card.getStatus())) {
            failPayment(payment, "Card is blocked");
            return;
        }

        // 5. Check balance
        if (card.getBalance().compareTo(amount) < 0) {
            failPayment(payment, "Insufficient funds");
            return;
        }

        // 6. Deduct atomically and mark SUCCESS
        card.setBalance(card.getBalance().subtract(amount));
        cardRepository.save(card);

        payment.setStatus("SUCCESS");
        payment.setProcessedAt(OffsetDateTime.now());
        paymentRepository.save(payment);

        log.info("Payment {} processed successfully: amount={} cardId={}", payment.getId(), amount, card.getId());

        publishResult(payment);
        publishStatus(payment);
    }

    private void failPayment(Payment payment, String reason) {
        payment.setStatus("FAILED");
        payment.setFailureReason(reason);
        payment.setProcessedAt(OffsetDateTime.now());
        paymentRepository.save(payment);

        log.warn("Payment {} failed: {}", payment.getId(), reason);

        publishResult(payment);
        publishStatus(payment);
    }

    private void publishResult(Payment payment) {
        producer.publishResult(PaymentResultEvent.builder()
                .paymentId(payment.getId().toString())
                .rentalId(payment.getRentalId())
                .status(payment.getStatus())
                .amount(payment.getAmount().toPlainString())
                .processedAt(payment.getProcessedAt() != null
                        ? payment.getProcessedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                        : null)
                .failureReason(payment.getFailureReason())
                .build());
    }

    private void publishStatus(Payment payment) {
        producer.publishStatus(PaymentStatusEvent.builder()
                .paymentId(payment.getId().toString())
                .rentalId(payment.getRentalId())
                .userId(payment.getUserId())
                .status(payment.getStatus())
                .type(payment.getType())
                .amount(payment.getAmount().toPlainString())
                .timestamp(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .build());
    }
}
