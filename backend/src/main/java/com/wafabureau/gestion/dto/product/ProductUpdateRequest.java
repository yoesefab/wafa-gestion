package com.wafabureau.gestion.dto.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import com.wafabureau.gestion.enums.UnitOfMeasure;

public record ProductUpdateRequest(
        @NotBlank @Size(max = 80) String sku,
        @NotBlank @Size(max = 180) String name,
        @NotNull @Positive Long categoryId,
        @NotNull UnitOfMeasure unitOfMeasure,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal purchasePrice,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal sellingPrice,
        @NotNull @PositiveOrZero Long minimumStock,
        @NotNull @PositiveOrZero Long version
) {
}
