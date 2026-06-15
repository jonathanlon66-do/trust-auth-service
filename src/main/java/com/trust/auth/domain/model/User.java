package com.trust.auth.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class User {
    private final String userId;
    private final String cognitoSub;
    private final String email;
    private final String name;
    private final String phone;
    private final String documentNumber;
    private final boolean onboarded;
    private final boolean active;
    private final PlatformRole platformRole;
    private final Instant createdAt;
}
