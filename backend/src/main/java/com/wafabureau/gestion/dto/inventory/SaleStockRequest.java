package com.wafabureau.gestion.dto.inventory;

public record SaleStockRequest(
        Long productId,
        Long salesOrderItemId,
        long quantity,
        String reference
) {
}
