package com.trust.auth.domain.port.in;

import reactor.core.publisher.Mono;

public interface LogoutUseCase {

    /**
     * UC-03: Invalida el access_token agregándolo a la blacklist con TTL.
     */
    Mono<Void> logout(String accessToken);
}
