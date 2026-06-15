package com.trust.auth.domain.port.in;

import reactor.core.publisher.Mono;

public interface ConfirmForgotPasswordUseCase {

    /**
     * UC-07: Confirma el reset de contraseña con el código recibido por email.
     */
    Mono<Void> confirmForgotPassword(String email, String confirmationCode, String newPassword);
}
