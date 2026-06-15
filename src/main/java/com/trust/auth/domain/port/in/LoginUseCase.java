package com.trust.auth.domain.port.in;

import com.trust.auth.domain.model.LoginResult;
import reactor.core.publisher.Mono;

public interface LoginUseCase {

    /**
     * UC-01: Autentica un usuario con sus credenciales y el código de empresa.
     * Si Cognito retorna NEW_PASSWORD_REQUIRED, el resultado tendrá
     * status = REQUIRES_ONBOARDING con el session_token para el onboarding.
     */
    Mono<LoginResult> login(String companyCode, String email, String password);
}
