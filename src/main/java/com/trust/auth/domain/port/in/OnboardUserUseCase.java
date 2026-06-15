package com.trust.auth.domain.port.in;

import com.trust.auth.domain.model.OnboardRequest;
import com.trust.auth.domain.model.TokenPair;
import reactor.core.publisher.Mono;

public interface OnboardUserUseCase {

    /**
     * UC-05: Completa el perfil del usuario en su primer login.
     * Responde al challenge NEW_PASSWORD_REQUIRED de Cognito,
     * guarda los datos personales y marca onboarded = true.
     * Solo puede ejecutarse una vez por usuario.
     */
    Mono<TokenPair> onboard(OnboardRequest request);
}
