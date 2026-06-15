package com.trust.auth.domain.port.in;

import com.trust.auth.domain.model.UserCda;
import reactor.core.publisher.Flux;

public interface ListWorkersUseCase {

    /**
     * UC-09: Lista todos los trabajadores activos de un CDA.
     * Requiere scope ADMINISTRADOR en el caller.
     */
    Flux<UserCda> listWorkers(String cdaId);
}
