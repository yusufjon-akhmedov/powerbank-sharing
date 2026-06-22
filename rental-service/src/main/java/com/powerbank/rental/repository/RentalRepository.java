package com.powerbank.rental.repository;

import com.powerbank.rental.entity.Rental;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RentalRepository extends JpaRepository<Rental, UUID> {
    Optional<Rental> findByIdempotencyKey(String idempotencyKey);
    List<Rental> findAllByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    List<Rental> findAllByStatus(String status);
}
