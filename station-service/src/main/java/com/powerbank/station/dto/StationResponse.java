package com.powerbank.station.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StationResponse {
    private String id;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private int totalSlots;
    private int availableSlots;
    private String status;
}
