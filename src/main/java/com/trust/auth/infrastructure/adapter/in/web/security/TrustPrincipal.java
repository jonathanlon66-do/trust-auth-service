package com.trust.auth.infrastructure.adapter.in.web.security;

import java.util.List;

public record TrustPrincipal(String userId, String cdaId, List<String> scopes) {}
