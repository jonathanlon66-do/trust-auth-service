package com.trust.auth.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class UserCda {

    public static final String DEFAULT_ADMIN_ROLE = "Administrador";

    private final String userId;
    private final String cdaId;
    private final String email;
    private final String name;
    private final String role;
    private final List<Scope> scopes;
    private final boolean active;
    private final Instant createdAt;

    public static UserCda createAdmin(String userId, String cdaId) {
        return UserCda.builder()
                .userId(userId)
                .cdaId(cdaId)
                .role(DEFAULT_ADMIN_ROLE)
                .scopes(List.of(Scope.values()))
                .active(true)
                .createdAt(Instant.now())
                .build();
    }

    public static UserCda createWorker(String userId, String cdaId, String role, List<Scope> scopes) {
        return UserCda.builder()
                .userId(userId)
                .cdaId(cdaId)
                .role(role)
                .scopes(scopes)
                .active(true)
                .createdAt(Instant.now())
                .build();
    }
}
