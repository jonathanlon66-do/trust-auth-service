package com.trust.auth.domain.port.in;

import com.trust.auth.domain.model.TokenClaims;
import reactor.core.publisher.Mono;

public interface ValidateTokenUseCase {

    /**
     * UC-12: Valida un JWT de Trust y retorna sus claims.
     * Usado por otros microservicios internamente.
     * Verifica firma, expiración y blacklist.
     */
    Mono<TokenClaims> validateToken(String accessToken);
}
