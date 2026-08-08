package com.eventflow.auth.api.dto;

import java.util.Set;

public record CurrentUserResponse(
        Long id,
        String username,
        String displayName,
        String avatarUrl,
        String mobile,
        String email,
        Long organizationId,
        Set<String> roles) {}
