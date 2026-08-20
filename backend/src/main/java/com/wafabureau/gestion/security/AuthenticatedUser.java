package com.wafabureau.gestion.security;
import com.wafabureau.gestion.model.*;
import com.wafabureau.gestion.repository.*;
import com.wafabureau.gestion.exception.*;
import com.wafabureau.gestion.dto.common.*;
import com.wafabureau.gestion.util.*;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.wafabureau.gestion.model.User;

public record AuthenticatedUser(
        Long id,
        String firstName,
        String lastName,
        String email,
        String password,
        boolean active,
        Instant createdAt
) implements UserDetails {

    static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPassword(),
                user.isActive(),
                user.getCreatedAt()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
