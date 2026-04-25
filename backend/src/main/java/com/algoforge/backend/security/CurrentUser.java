package com.algoforge.backend.security;

import lombok.Builder;

import java.util.Collection;

@Builder
public record CurrentUser(Long userId, Collection<String> roles) {
    public boolean isAdmin() {
        return roles.contains("ROLE_ADMIN");
    }
}
