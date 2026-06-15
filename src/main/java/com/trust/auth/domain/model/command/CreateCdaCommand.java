package com.trust.auth.domain.model.command;

public record CreateCdaCommand(
        String name,
        String companyCode,
        String adminEmail,
        String adminName
) {}
