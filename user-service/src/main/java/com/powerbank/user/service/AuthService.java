package com.powerbank.user.service;

import com.powerbank.user.dto.TokenResponse;
import com.powerbank.user.entity.User;
import com.powerbank.user.repository.UserRepository;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final OtpService otpService;
    private final UserRepository userRepository;
    private final Keycloak keycloakAdmin;
    private final RestTemplate restTemplate;

    @Value("${keycloak.server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    public void sendOtp(String phone) {
        otpService.generateAndStore(phone);
    }

    @Transactional
    public TokenResponse verify(String phone, String otp) {
        if (!otpService.validate(phone, otp)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired OTP");
        }

        userRepository.findByPhone(phone).orElseGet(() -> {
            User newUser = new User();
            newUser.setPhone(phone);
            return userRepository.save(newUser);
        });

        ensureKeycloakUser(phone);
        return getTokenForUser(phone);
    }

    public TokenResponse refresh(String refreshToken) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "refresh_token");
        params.add("client_id", clientId);
        params.add("refresh_token", refreshToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                tokenEndpoint(),
                new HttpEntity<>(params, headers),
                Map.class
        );

        return mapTokenResponse(response.getBody());
    }

    private void ensureKeycloakUser(String phone) {
        RealmResource realmResource = keycloakAdmin.realm(realm);
        List<UserRepresentation> existing = realmResource.users().searchByUsername(phone, true);
        if (!existing.isEmpty()) {
            return;
        }

        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername(phone);
        userRep.setEnabled(true);

        Response createResponse = realmResource.users().create(userRep);
        if (createResponse.getStatus() != 201) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to create Keycloak user, status=" + createResponse.getStatus());
        }

        String location = createResponse.getHeaderString("Location");
        String userId = location.substring(location.lastIndexOf('/') + 1);

        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(derivePassword(phone));
        cred.setTemporary(false);
        realmResource.users().get(userId).resetPassword(cred);
    }

    private TokenResponse getTokenForUser(String phone) {
        Keycloak userKeycloak = KeycloakBuilder.builder()
                .serverUrl(keycloakServerUrl)
                .realm(realm)
                .clientId(clientId)
                .username(phone)
                .password(derivePassword(phone))
                .grantType(OAuth2Constants.PASSWORD)
                .build();

        AccessTokenResponse token = userKeycloak.tokenManager().getAccessToken();
        return TokenResponse.builder()
                .accessToken(token.getToken())
                .refreshToken(token.getRefreshToken())
                .expiresIn(token.getExpiresIn())
                .build();
    }

    @SuppressWarnings("unchecked")
    private TokenResponse mapTokenResponse(Map body) {
        return TokenResponse.builder()
                .accessToken((String) body.get("access_token"))
                .refreshToken((String) body.get("refresh_token"))
                .expiresIn(body.get("expires_in") instanceof Number n ? n.longValue() : 0L)
                .build();
    }

    // Deterministic internal password — Keycloak is not the auth factor; OTP is.
    private String derivePassword(String phone) {
        return phone + "_pb_internal";
    }

    private String tokenEndpoint() {
        return keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";
    }
}
