package com.trust.auth.domain.port.in;

import com.trust.auth.domain.model.Scope;
import com.trust.auth.domain.model.UserCda;
import reactor.core.publisher.Mono;

import java.util.List;

public interface UpdateWorkerUseCase {

    /**
     * UC-10: Actualiza el rol y scopes de un trabajador dentro del CDA.
     * Requiere scope ADMINISTRADOR.
     * Un admin no puede quitarse el scope ADMINISTRADOR a sí mismo.
     */
    Mono<UserCda> updateWorker(String callerUserId, String callerCdaId,
                               String targetUserId, String roleName, List<Scope> scopes);
}
