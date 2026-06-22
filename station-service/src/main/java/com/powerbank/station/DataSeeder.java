package com.powerbank.station;

import com.powerbank.station.entity.PowerBank;
import com.powerbank.station.entity.Station;
import com.powerbank.station.repository.PowerBankRepository;
import com.powerbank.station.repository.StationRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    private final StationRepository stationRepository;
    private final PowerBankRepository powerBankRepository;

    @PostConstruct
    public void seed() {
        if (stationRepository.count() > 0) {
            return;
        }

        log.info("Seeding sample stations...");

        seedStation("Amir Temur Maydoni", "Amir Temur ko'chasi 1, Tashkent",
                new BigDecimal("41.299500"), new BigDecimal("69.240100"));

        seedStation("Yunusabad Metro", "Yunusabad tumani, Tashkent",
                new BigDecimal("41.311100"), new BigDecimal("69.279700"));

        seedStation("Chilanzar DC", "Chilanzar tumani, Tashkent",
                new BigDecimal("41.281400"), new BigDecimal("69.159200"));

        log.info("Seeded 3 stations with 5 powerbanks each.");
    }

    private void seedStation(String name, String address, BigDecimal lat, BigDecimal lng) {
        Station station = new Station();
        station.setName(name);
        station.setAddress(address);
        station.setLatitude(lat);
        station.setLongitude(lng);
        station.setTotalSlots(5);
        station.setAvailableSlots(5);
        station.setStatus("ACTIVE");
        stationRepository.save(station);

        for (int slot = 1; slot <= 5; slot++) {
            PowerBank pb = new PowerBank();
            pb.setStationId(station.getId());
            pb.setSlotNumber(slot);
            pb.setStatus("AVAILABLE");
            pb.setBatteryLevel(100);
            powerBankRepository.save(pb);
        }
    }
}
