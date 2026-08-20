package com.wafabureau.gestion.dto.inventory;

public record PurchaseStockRequest(
        Long productId,
        Long purchaseOrderItemId,
        long quantity,
        String reference
) {
}
