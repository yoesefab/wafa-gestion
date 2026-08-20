package com.wafabureau.gestion.dto.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import com.wafabureau.gestion.enums.AdjustmentDirection;

public record ManualStockAdjustmentRequest(
        @NotNull AdjustmentDirection direction,
        @Positive long quantity,
        @NotBlank @Size(max = 120) String reference,
        @NotBlank @Size(max = 180) String reason,
        @Size(max = 1000) String note
) {

    public ManualStockAdjustmentRequest {
        reference = trim(reference);
        reason = trim(reason);
        note = trim(note);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
