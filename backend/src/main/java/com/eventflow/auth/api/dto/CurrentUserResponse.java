package com.eventflow.auth.api.dto;

import java.util.Set;

public record CurrentUserResponse(
        Long id, String username, String displayName, Long organizationId, Set<String> roles) {}
