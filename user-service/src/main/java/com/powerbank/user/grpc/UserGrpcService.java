package com.powerbank.user.grpc;

import com.powerbank.proto.user.GetUserRequest;
import com.powerbank.proto.user.GetUserResponse;
import com.powerbank.proto.user.UserServiceGrpc;
import com.powerbank.proto.user.ValidateTokenRequest;
import com.powerbank.proto.user.ValidateTokenResponse;
import com.powerbank.user.entity.User;
import com.powerbank.user.repository.UserRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${keycloak.server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Override
    public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
        try {
            UUID userId = UUID.fromString(request.getUserId());
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> Status.NOT_FOUND
                            .withDescription("User not found: " + request.getUserId())
                            .asRuntimeException());

            responseObserver.onNext(GetUserResponse.newBuilder()
                    .setUserId(user.getId().toString())
                    .setPhone(user.getPhone())
                    .setStatus(user.getStatus())
                    .build());
            responseObserver.onCompleted();
        } catch (io.grpc.StatusRuntimeException e) {
            responseObserver.onError(e);
        } catch (Exception e) {
            log.error("getUser failed", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void validateToken(ValidateTokenRequest request, StreamObserver<ValidateTokenResponse> responseObserver) {
        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("token", request.getToken());
            params.add("client_id", clientId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token/introspect",
                    new HttpEntity<>(params, headers),
                    Map.class
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            boolean active = Boolean.TRUE.equals(body != null ? body.get("active") : false);

            ValidateTokenResponse.Builder builder = ValidateTokenResponse.newBuilder().setValid(active);
            if (active && body != null) {
                builder.setPhone(String.valueOf(body.getOrDefault("preferred_username", "")));
                builder.setUserId(String.valueOf(body.getOrDefault("sub", "")));
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("validateToken failed", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }
}
