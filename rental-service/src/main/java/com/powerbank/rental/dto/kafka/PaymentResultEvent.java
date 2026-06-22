package com.powerbank.rental.dto.kafka;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PaymentResultEvent {
    private String paymentId;
    private String rentalId;
    private String status;
    private String amount;
    private String processedAt;
    private String failureReason;
}
