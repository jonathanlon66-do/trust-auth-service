package com.trust.auth.domain.port.in;

import reactor.core.publisher.Mono;

public interface DeactivateWorkerUseCase {

    /**
     * UC-11: Desactiva un trabajador en el CDA (no lo elimina del sistema).
     * Requiere scope ADMINISTRADOR.
     * Un admin no puede desactivarse a sí mismo.
     */
    Mono<Void> deactivateWorker(String callerUserId, String callerCdaId, String targetUserId);
}
