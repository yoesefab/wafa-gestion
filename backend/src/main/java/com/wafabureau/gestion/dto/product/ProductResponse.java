package com.wafabureau.gestion.dto.product;

import com.wafabureau.gestion.dto.category.CategoryReference;

import java.math.BigDecimal;
import java.time.Instant;

import com.wafabureau.gestion.enums.UnitOfMeasure;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        CategoryReference category,
        UnitOfMeasure unitOfMeasure,
        BigDecimal purchasePrice,
        BigDecimal sellingPrice,
        Long currentStock,
        Long minimumStock,
        boolean lowStock,
        boolean active,
        Instant deactivatedAt,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
}
