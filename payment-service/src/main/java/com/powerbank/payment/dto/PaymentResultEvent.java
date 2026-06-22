package com.powerbank.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResultEvent {
    private String paymentId;
    private String rentalId;
    private String status;
    private String amount;
    private String processedAt;
    private String failureReason;
}
