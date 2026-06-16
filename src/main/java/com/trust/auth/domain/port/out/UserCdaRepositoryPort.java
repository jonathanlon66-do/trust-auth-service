package com.trust.auth.domain.port.out;

import com.trust.auth.domain.model.UserCda;
import reactor.core.publisher.Mono;

public interface UserCdaRepositoryPort {
    Mono<Boolean> exists(String cdaId, String userId);
    Mono<UserCda> save(UserCda userCda);
}
