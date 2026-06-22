package com.powerbank.payment;

import com.powerbank.payment.entity.Card;
import com.powerbank.payment.repository.CardRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    private final CardRepository cardRepository;

    @PostConstruct
    public void seed() {
        if (cardRepository.count() > 0) {
            return;
        }

        log.info("Seeding test cards...");

        Card card1 = new Card();
        card1.setUserId("test-user-1");
        card1.setCardNumber("8600123456789012");
        card1.setHolderName("Test User One");
        card1.setBalance(new BigDecimal("500000.00"));
        card1.setCurrency("UZS");
        card1.setStatus("ACTIVE");
        cardRepository.save(card1);

        // Low-balance card — for insufficient funds test
        Card card2 = new Card();
        card2.setUserId("test-user-2");
        card2.setCardNumber("8600987654321098");
        card2.setHolderName("Test User Two");
        card2.setBalance(new BigDecimal("100.00"));
        card2.setCurrency("UZS");
        card2.setStatus("ACTIVE");
        cardRepository.save(card2);

        log.info("Seeded 2 test cards.");
    }
}
