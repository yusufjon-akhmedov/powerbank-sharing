package com.powerbank.station.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcquireLockResult {
    private String rentalId;
    private String stationId;
    private String slotId;
    private boolean success;
    private String message;
}
