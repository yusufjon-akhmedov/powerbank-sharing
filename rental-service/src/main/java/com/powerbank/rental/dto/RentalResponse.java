package com.powerbank.rental.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RentalResponse {
    private String rentalId;
    private String status;
    private String powerBankId;
    private String slotId;
    private OffsetDateTime startedAt;
    private BigDecimal totalAmount;
    private String message;
    private boolean success;
}
