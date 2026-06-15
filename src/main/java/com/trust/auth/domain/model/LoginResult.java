package com.trust.auth.domain.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResult {

    public enum Status { SUCCESS, REQUIRES_ONBOARDING }

    private final Status status;

    // Cuando status = SUCCESS
    private final String accessToken;
    private final String refreshToken;
    private final long expiresIn;

    // Cuando status = REQUIRES_ONBOARDING
    private final String sessionToken;
}
