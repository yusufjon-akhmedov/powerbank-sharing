package com.powerbank.rental.dto.kafka;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AcquireLockResult {
    private String rentalId;
    private String stationId;
    private String slotId;
    private boolean success;
    private String message;
}
