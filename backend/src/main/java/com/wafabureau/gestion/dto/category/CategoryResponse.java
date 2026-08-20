package com.wafabureau.gestion.dto.category;

import java.time.Instant;


public record CategoryResponse(
        Long id,
        String name,
        String description,
        boolean active,
        Instant deactivatedAt,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
}
