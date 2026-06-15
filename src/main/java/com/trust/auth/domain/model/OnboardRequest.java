package com.trust.auth.domain.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OnboardRequest {
    private final String sessionToken;
    private final String newPassword;
    private final String name;
    private final String phone;
    private final String documentNumber;
}
