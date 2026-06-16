package com.trust.auth.domain.model.result;

import com.trust.auth.domain.model.Scope;

import java.util.List;

public record WorkerCreatedResult(
        String userId,
        String email,
        String role,
        List<Scope> scopes,
        String status
) {
    public static WorkerCreatedResult invited(String userId, String email, String role, List<Scope> scopes) {
        return new WorkerCreatedResult(userId, email, role, scopes, "INVITED");
    }
}
