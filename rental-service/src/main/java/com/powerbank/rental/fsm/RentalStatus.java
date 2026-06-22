package com.powerbank.rental.fsm;

public enum RentalStatus {
    WAITING,
    LOCKING_STATION,
    PROCESSING_PAYMENT,
    EJECTING_POWERBANK,
    IN_THE_LEASE,
    FINISHING,
    DONE,
    FAILED
}
