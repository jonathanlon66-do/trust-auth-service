package com.trust.auth.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TokenClaims {
    private final String userId;
    private final String cdaId;
    private final String role;
    private final List<Scope> scopes;
    private final String email;
}
