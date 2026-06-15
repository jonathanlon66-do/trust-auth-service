package com.trust.auth.domain.port.out;

import com.trust.auth.domain.model.UserCda;
import reactor.core.publisher.Mono;

public interface UserCdaRepositoryPort {
    Mono<UserCda> save(UserCda userCda);
}
