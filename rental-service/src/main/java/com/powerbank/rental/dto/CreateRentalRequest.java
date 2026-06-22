package com.powerbank.rental.dto;

import lombok.Data;

@Data
public class CreateRentalRequest {
    private String stationId;
    private String cardId;
    private String idempotencyKey;
}
