package com.powerbank.rental.fsm;

import com.powerbank.rental.entity.Rental;
import com.powerbank.rental.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class RentalFSM {

    private final RentalRepository rentalRepository;

    // Valid forward transitions
    private static final Map<RentalStatus, Set<RentalStatus>> ALLOWED = Map.of(
            RentalStatus.WAITING,              Set.of(RentalStatus.LOCKING_STATION, RentalStatus.FAILED),
            RentalStatus.LOCKING_STATION,      Set.of(RentalStatus.PROCESSING_PAYMENT, RentalStatus.FAILED),
            RentalStatus.PROCESSING_PAYMENT,   Set.of(RentalStatus.EJECTING_POWERBANK, RentalStatus.FAILED),
            RentalStatus.EJECTING_POWERBANK,   Set.of(RentalStatus.IN_THE_LEASE, RentalStatus.FAILED),
            RentalStatus.IN_THE_LEASE,         Set.of(RentalStatus.FINISHING, RentalStatus.FAILED),
            RentalStatus.FINISHING,            Set.of(RentalStatus.DONE, RentalStatus.FAILED),
            RentalStatus.DONE,                 Set.of(),
            RentalStatus.FAILED,               Set.of()
    );

    public void transitionTo(Rental rental, RentalStatus newStatus) {
        RentalStatus current = RentalStatus.valueOf(rental.getStatus());
        Set<RentalStatus> allowed = ALLOWED.getOrDefault(current, Set.of());

        if (!allowed.contains(newStatus)) {
            throw new IllegalStateException(
                    "Invalid FSM transition: " + current + " → " + newStatus +
                    " for rental " + rental.getId());
        }

        log.info("Rental {} FSM: {} → {}", rental.getId(), current, newStatus);
        rental.setStatus(newStatus.name());
        rentalRepository.save(rental);
    }

    public boolean isTerminal(RentalStatus status) {
        return status == RentalStatus.DONE || status == RentalStatus.FAILED;
    }
}
