package com.powerbank.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusEvent {
    private String paymentId;
    private String rentalId;
    private String userId;
    private String status;
    private String type;
    private String amount;
    private String timestamp;
}
