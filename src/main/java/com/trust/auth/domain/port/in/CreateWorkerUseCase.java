package com.trust.auth.domain.port.in;

import com.trust.auth.domain.model.Scope;
import com.trust.auth.domain.model.UserCda;
import reactor.core.publisher.Mono;

import java.util.List;

public interface CreateWorkerUseCase {

    /**
     * UC-08: Admin del CDA crea un trabajador.
     * Si el email ya existe en el sistema (otro CDA), no crea cuenta en Cognito,
     * solo vincula al usuario con este CDA.
     * Requiere scope ADMINISTRADOR en el caller.
     */
    Mono<UserCda> createWorker(String callerCdaId, String email,
                               String name, String roleName, List<Scope> scopes);
}
