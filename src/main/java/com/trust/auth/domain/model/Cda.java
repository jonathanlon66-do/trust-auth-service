package com.trust.auth.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class Cda {
    private final String cdaId;
    private final String companyCode;
    private final String name;
    private final boolean active;
    private final Instant createdAt;

    public static Cda create(String cdaId, String companyCode, String name) {
        return Cda.builder()
                .cdaId(cdaId)
                .companyCode(companyCode)
                .name(name)
                .active(true)
                .createdAt(Instant.now())
                .build();
    }
}
