package com.trust.auth.infrastructure.adapter.in.web.dto.response;

import java.util.List;

public record CreateWorkerResponse(
        String userId,
        String email,
        String role,
        List<String> scopes,
        String status
) {}
