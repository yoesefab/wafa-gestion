package com.wafabureau.gestion.dto.auth;

import java.time.Instant;

import com.wafabureau.gestion.security.AuthenticatedUser;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        boolean active,
        Instant createdAt
) {
}
