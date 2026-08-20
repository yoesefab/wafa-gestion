package com.wafabureau.gestion.mapper;

import com.wafabureau.gestion.dto.auth.UserResponse;
import com.wafabureau.gestion.security.AuthenticatedUser;

public final class AuthMapper {
    private AuthMapper() { }

    public static UserResponse toResponse(AuthenticatedUser user) {
        return new UserResponse(
                user.id(), user.firstName(), user.lastName(), user.email(), user.active(), user.createdAt()
        );
    }
}
