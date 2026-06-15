package com.trust.auth.domain.port.out;

import reactor.core.publisher.Mono;

public interface CognitoPort {
    Mono<String> adminCreateUser(String email, String tempPassword, String name);
    Mono<Void> deleteUser(String email);
}
