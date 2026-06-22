package com.powerbank.station.repository;

import com.powerbank.station.entity.PowerBank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PowerBankRepository extends JpaRepository<PowerBank, UUID> {
    Optional<PowerBank> findFirstByStationIdAndStatus(UUID stationId, String status);
    long countByStationId(UUID stationId);
}
