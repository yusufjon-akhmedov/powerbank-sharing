package com.powerbank.rental.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EjectEvent {
    private String rentalId;
    private String stationId;
    private String slotId;
}
