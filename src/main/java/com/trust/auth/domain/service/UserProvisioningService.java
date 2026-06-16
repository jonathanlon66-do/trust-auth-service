package com.trust.auth.domain.service;

import com.trust.auth.domain.model.User;
import com.trust.auth.domain.model.result.ProvisionedUser;
import com.trust.auth.domain.port.out.CognitoPort;
import com.trust.auth.domain.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProvisioningService {

    private final CognitoPort cognito;
    private final UserRepositoryPort userRepository;

    public Mono<ProvisionedUser> provision(String email, String name) {
        String userId = UUID.randomUUID().toString();
        String tempPassword = generateTempPassword();

        return cognito.adminCreateUser(email, tempPassword, name)
                .doOnSuccess(sub -> log.debug("Usuario creado en Cognito: userId={}", userId))
                .flatMap(cognitoSub -> userRepository.save(User.createPending(userId, cognitoSub, email, name))
                        .onErrorResume(e -> cognito.deleteUser(email).then(Mono.error(e))))
                .doOnSuccess(u -> log.debug("Usuario guardado en DynamoDB: userId={}", userId))
                .map(user -> new ProvisionedUser(user, tempPassword));
    }

    public Mono<Void> deprovision(String email, String userId) {
        return userRepository.delete(userId)
                .then(cognito.deleteUser(email))
                .doOnSuccess(v -> log.warn("Usuario desaprovisionado (rollback): userId={}", userId));
    }

    private String generateTempPassword() {
        byte[] bytes = new byte[12];
        new SecureRandom().nextBytes(bytes);
        String base = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return "Tr1!" + base.substring(0, 9);
    }
}
