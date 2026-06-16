package com.trust.auth.domain.model.result;

import com.trust.auth.domain.model.User;

public record ProvisionedUser(User user, String tempPassword) {}
