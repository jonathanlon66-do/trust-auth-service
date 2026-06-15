package com.trust.auth.infrastructure.adapter.in.web.dto.response;

public record CreateCdaResponse(
        String cdaId,
        String companyCode,
        String adminEmail,
        String status
) {}
