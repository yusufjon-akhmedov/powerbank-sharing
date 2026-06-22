package com.powerbank.station.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NearbyStationsRequest {
    private double latitude;
    private double longitude;
    private double radiusKm;
}
