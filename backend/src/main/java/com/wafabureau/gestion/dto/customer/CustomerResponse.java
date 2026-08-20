package com.wafabureau.gestion.dto.customer;

import java.time.Instant;


public record CustomerResponse(
        Long id,
        String name,
        String ice,
        String contactPerson,
        String email,
        String phone,
        String address,
        boolean active,
        Instant deactivatedAt,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
}
