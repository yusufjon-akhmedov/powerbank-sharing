package com.powerbank.station.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EjectResult {
    private String rentalId;
    private String powerBankId;
    private boolean success;
    private String message;
}
