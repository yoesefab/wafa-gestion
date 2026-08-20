package com.wafabureau.gestion.dto.purchase;

import java.math.BigDecimal;


public record PurchaseOrderItemResponse(
        Long id,
        PurchaseProductResponse product,
        Long quantity,
        BigDecimal unitPrice,
        BigDecimal taxRate,
        BigDecimal lineSubtotal,
        BigDecimal lineTax,
        BigDecimal lineTotal
) {
}
