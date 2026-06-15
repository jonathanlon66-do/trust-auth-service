package com.trust.auth.domain.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenPair {
    private final String accessToken;
    private final String refreshToken;
    private final long expiresIn;
}
