package com.eventflow.shared.security;

import java.util.Set;

public record AuthenticatedPrincipal(long userId, Long organizationId, Set<String> roles) {

    public AuthenticatedPrincipal {
        roles = Set.copyOf(roles);
    }
}
