package com.trust.auth.domain.port.out;

import com.trust.auth.domain.model.User;
import reactor.core.publisher.Mono;

public interface UserRepositoryPort {
    Mono<User> save(User user);
    Mono<Void> delete(String userId);
}
