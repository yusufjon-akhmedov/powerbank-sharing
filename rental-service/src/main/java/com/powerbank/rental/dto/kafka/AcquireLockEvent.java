package com.powerbank.rental.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcquireLockEvent {
    private String rentalId;
    private String stationId;
}
