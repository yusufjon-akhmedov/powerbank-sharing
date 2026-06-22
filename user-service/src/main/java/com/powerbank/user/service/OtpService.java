package com.powerbank.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class OtpService {

    private final ConcurrentHashMap<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
    private final Random random = new Random();

    @Value("${otp.ttl-minutes:5}")
    private int ttlMinutes;

    public void generateAndStore(String phone) {
        String otp = String.format("%06d", random.nextInt(1_000_000));
        otpStore.put(phone, new OtpEntry(otp, LocalDateTime.now().plusMinutes(ttlMinutes)));
        log.info("OTP for {}: {}", phone, otp);
    }

    public boolean validate(String phone, String otp) {
        OtpEntry entry = otpStore.get(phone);
        if (entry == null || entry.isExpired()) {
            otpStore.remove(phone);
            return false;
        }
        if (entry.otp().equals(otp)) {
            otpStore.remove(phone);
            return true;
        }
        return false;
    }

    private record OtpEntry(String otp, LocalDateTime expiresAt) {
        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }
}
