package com.powerbank.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
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
