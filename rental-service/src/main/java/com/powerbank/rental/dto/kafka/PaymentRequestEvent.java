package com.powerbank.rental.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestEvent {
    private String paymentId;
    private String rentalId;
    private String cardId;
    private String userId;
    private String amount;
    private String idempotencyKey;
    private String type;
}
