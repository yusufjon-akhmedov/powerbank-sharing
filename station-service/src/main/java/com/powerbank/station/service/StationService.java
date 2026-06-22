package com.powerbank.station.service;

import com.powerbank.station.dto.StationResponse;
import com.powerbank.station.entity.PowerBank;
import com.powerbank.station.entity.Station;
import com.powerbank.station.repository.PowerBankRepository;
import com.powerbank.station.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StationService {

    private final StationRepository stationRepository;
    private final PowerBankRepository powerBankRepository;

    public StationResponse getStation(UUID stationId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new RuntimeException("Station not found: " + stationId));
        return toResponse(station);
    }

    // Simplified: returns all ACTIVE stations regardless of radius.
    // TODO: replace with PostGIS ST_DWithin for true geo-radius filtering.
    public List<StationResponse> getNearbyStations(double latitude, double longitude, double radiusKm) {
        return stationRepository.findAllByStatus("ACTIVE")
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public String lockSlot(UUID stationId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new RuntimeException("Station not found: " + stationId));

        if (station.getAvailableSlots() <= 0) {
            throw new RuntimeException("No available slots at station: " + stationId);
        }

        PowerBank powerBank = powerBankRepository
                .findFirstByStationIdAndStatus(stationId, "AVAILABLE")
                .orElseThrow(() -> new RuntimeException("No available powerbank at station: " + stationId));

        powerBank.setStatus("IN_USE");
        powerBankRepository.save(powerBank);

        station.setAvailableSlots(station.getAvailableSlots() - 1);
        stationRepository.save(station);

        log.info("Locked slot {} at station {}", powerBank.getId(), stationId);
        return powerBank.getId().toString();
    }

    @Transactional
    public String returnPowerBank(UUID powerBankId, UUID stationId) {
        PowerBank powerBank = powerBankRepository.findById(powerBankId)
                .orElseThrow(() -> new RuntimeException("PowerBank not found: " + powerBankId));

        powerBank.setStatus("AVAILABLE");
        powerBankRepository.save(powerBank);

        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new RuntimeException("Station not found: " + stationId));
        station.setAvailableSlots(station.getAvailableSlots() + 1);
        stationRepository.save(station);

        log.info("Returned powerbank {} to station {}", powerBankId, stationId);
        return powerBank.getId().toString();
    }

    private StationResponse toResponse(Station s) {
        return StationResponse.builder()
                .id(s.getId().toString())
                .name(s.getName())
                .address(s.getAddress())
                .latitude(s.getLatitude().doubleValue())
                .longitude(s.getLongitude().doubleValue())
                .totalSlots(s.getTotalSlots())
                .availableSlots(s.getAvailableSlots())
                .status(s.getStatus())
                .build();
    }
}
