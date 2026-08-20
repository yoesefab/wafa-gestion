package com.wafabureau.gestion.dto.purchase;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PurchaseOrderWriteRequest(
        @NotNull @Positive Long supplierId,
        @NotNull LocalDate orderDate,
        @Size(max = 1000) String note,
        @NotEmpty List<@Valid PurchaseOrderLineWriteRequest> lines
) {
}
