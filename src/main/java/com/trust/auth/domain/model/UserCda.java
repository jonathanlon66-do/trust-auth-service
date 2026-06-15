package com.trust.auth.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class UserCda {
    private final String userId;
    private final String cdaId;
    private final String email;
    private final String name;
    private final String role;
    private final List<Scope> scopes;
    private final boolean active;
    private final Instant createdAt;
}
