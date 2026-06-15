package com.trust.auth.domain.port.in;

import com.trust.auth.domain.model.TokenPair;
import reactor.core.publisher.Mono;

public interface RefreshTokenUseCase {

    /**
     * UC-02: Renueva el access_token usando el refresh_token.
     * Valida que el usuario siga activo en el CDA del token.
     */
    Mono<TokenPair> refresh(String refreshToken);
}
