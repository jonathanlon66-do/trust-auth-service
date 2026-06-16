package com.trust.auth.domain.port.out;

import com.trust.auth.domain.model.Cda;
import reactor.core.publisher.Mono;

public interface CdaRepositoryPort {
    Mono<Boolean> existsByCompanyCode(String companyCode);
    Mono<Cda> findById(String cdaId);
    Mono<Cda> save(Cda cda);
    Mono<Void> delete(String cdaId);
}
