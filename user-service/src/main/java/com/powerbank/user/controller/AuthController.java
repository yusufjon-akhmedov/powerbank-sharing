package com.powerbank.user.controller;

import com.powerbank.user.dto.PhoneRequest;
import com.powerbank.user.dto.TokenResponse;
import com.powerbank.user.dto.VerifyRequest;
import com.powerbank.user.entity.User;
import com.powerbank.user.repository.UserRepository;
import com.powerbank.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/auth/phone")
    public ResponseEntity<Void> sendOtp(@RequestBody PhoneRequest request) {
        authService.sendOtp(request.getPhone());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/auth/verify")
    public ResponseEntity<TokenResponse> verify(@RequestBody VerifyRequest request) {
        return ResponseEntity.ok(authService.verify(request.getPhone(), request.getOtp()));
    }

    @PostMapping("/v1/auth/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.refresh(body.get("refreshToken")));
    }

    @GetMapping("/v1/me")
    public ResponseEntity<User> me(@AuthenticationPrincipal Jwt jwt) {
        String phone = jwt.getClaimAsString("preferred_username");
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return ResponseEntity.ok(user);
    }
}
