package com.wafabureau.gestion.dto.supplier;

import java.time.Instant;


public record SupplierResponse(
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
