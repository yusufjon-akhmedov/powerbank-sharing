package com.powerbank.rental.controller;

import com.powerbank.rental.dto.CreateRentalRequest;
import com.powerbank.rental.dto.FinishRentalRequest;
import com.powerbank.rental.dto.RentalResponse;
import com.powerbank.rental.entity.Rental;
import com.powerbank.rental.service.RentalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rentals")
@RequiredArgsConstructor
@Slf4j
public class RentalController {

    private final RentalService rentalService;

    @PostMapping
    public ResponseEntity<RentalResponse> createRental(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CreateRentalRequest request) {

        String userId = extractUserId(authHeader);

        Rental rental = rentalService.createOrReturnExisting(
                userId, request.getStationId(), request.getCardId(), request.getIdempotencyKey());

        boolean isNew = rental.getStatus().equals("WAITING")
                && rental.getCreatedAt() != null
                && rental.getCreatedAt().isAfter(java.time.OffsetDateTime.now().minusSeconds(5));

        if (!isNew && !rental.getStatus().equals("WAITING")) {
            // Idempotent replay — rental already exists
            return ResponseEntity.ok(rentalService.toResponse(rental));
        }

        rentalService.initiateRental(rental);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RentalResponse.builder()
                        .rentalId(rental.getId().toString())
                        .status(rental.getStatus())
                        .build());
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<RentalResponse> getStatus(@PathVariable String id) {
        Rental rental = rentalService.getById(id);
        return ResponseEntity.ok(rentalService.toResponse(rental));
    }

    @GetMapping("/history")
    public ResponseEntity<List<RentalResponse>> getHistory(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String userId = extractUserId(authHeader);
        List<Rental> rentals = rentalService.getHistory(userId, page, size);
        return ResponseEntity.ok(rentalService.toResponseList(rentals));
    }

    @PostMapping("/finish")
    public ResponseEntity<RentalResponse> finishRental(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody FinishRentalRequest request) {

        String userId = extractUserId(authHeader);
        RentalResponse response = rentalService.finishRental(
                request.getRentalId(), userId, request.getStationId());
        return ResponseEntity.ok(response);
    }

    /**
     * Parses the "sub" claim from a JWT without a full JWT library.
     * The token is not verified here — verification happens at the API gateway (Kong).
     */
    static String extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Malformed JWT");
        }
        try {
            // Pad base64url to standard base64 length
            String payload = parts[1];
            int pad = payload.length() % 4;
            if (pad == 2) payload += "==";
            else if (pad == 3) payload += "=";

            byte[] decoded = Base64.getUrlDecoder().decode(payload);
            String json = new String(decoded);

            // Simple extraction of "sub" without a JSON library dependency
            int subIdx = json.indexOf("\"sub\"");
            if (subIdx == -1) throw new IllegalArgumentException("No sub claim in JWT");
            int colon = json.indexOf(':', subIdx);
            int start = json.indexOf('"', colon) + 1;
            int end   = json.indexOf('"', start);
            return json.substring(start, end);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse JWT payload", e);
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException e) {
        log.error("Unhandled runtime error", e);
        return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
}
