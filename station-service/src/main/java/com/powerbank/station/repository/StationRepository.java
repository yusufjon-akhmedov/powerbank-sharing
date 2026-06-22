package com.powerbank.station.repository;

import com.powerbank.station.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StationRepository extends JpaRepository<Station, UUID> {
    List<Station> findAllByStatus(String status);
}
