package com.wafabureau.gestion.dto.inventory;

import java.time.Instant;

import com.wafabureau.gestion.enums.StockMovementType;

public record StockMovementResponse(
        Long id,
        ProductReference product,
        StockMovementType movementType,
        Long quantityDelta,
        Long stockBefore,
        Long stockAfter,
        String reference,
        String reason,
        String note,
        MovementActorResponse createdBy,
        Instant occurredAt
) {
}
