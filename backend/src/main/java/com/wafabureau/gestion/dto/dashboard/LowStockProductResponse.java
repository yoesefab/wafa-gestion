package com.wafabureau.gestion.dto.dashboard;

public record LowStockProductResponse(
        Long id,
        String sku,
        String name,
        long currentStock,
        long minimumStock
) {
}
