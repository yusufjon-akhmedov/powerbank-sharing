package com.powerbank.rental;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class RentalServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RentalServiceApplication.class, args);
    }
}
