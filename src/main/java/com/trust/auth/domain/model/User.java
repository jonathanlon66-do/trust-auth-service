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

    public static User createPending(String userId, String cognitoSub, String email, String name) {
        return User.builder()
                .userId(userId)
                .cognitoSub(cognitoSub)
                .email(email)
                .name(name)
                .onboarded(false)
                .active(true)
                .createdAt(Instant.now())
                .build();
    }
}
