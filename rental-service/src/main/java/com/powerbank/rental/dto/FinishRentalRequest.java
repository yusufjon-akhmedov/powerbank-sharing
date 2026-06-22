package com.powerbank.rental.dto;

import lombok.Data;

@Data
public class FinishRentalRequest {
    private String rentalId;
    private String stationId;
}
