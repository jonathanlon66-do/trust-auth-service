package com.trust.auth.domain.port.in;

import reactor.core.publisher.Mono;

public interface ForgotPasswordUseCase {

    /**
     * UC-06: Inicia el flujo de recuperación de contraseña.
     * Cognito envía un código de 6 dígitos al email del usuario.
     * Siempre retorna éxito para no revelar si el email existe.
     */
    Mono<Void> forgotPassword(String email);
}
