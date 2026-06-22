package com.powerbank.rental.dto.kafka;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EjectResult {
    private String rentalId;
    private String powerBankId;
    private boolean success;
    private String message;
}
