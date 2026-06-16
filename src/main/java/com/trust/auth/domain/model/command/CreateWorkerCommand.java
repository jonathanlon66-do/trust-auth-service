package com.trust.auth.domain.model.command;

import com.trust.auth.domain.model.Scope;

import java.util.List;

public record CreateWorkerCommand(
        String cdaId,
        String email,
        String name,
        String roleName,
        List<Scope> scopes
) {}
