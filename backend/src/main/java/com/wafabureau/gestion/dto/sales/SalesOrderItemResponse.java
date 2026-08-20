package com.wafabureau.gestion.dto.sales;

import java.math.BigDecimal;


public record SalesOrderItemResponse(
        Long id,
        SalesProductResponse product,
        Long quantity,
        BigDecimal unitPrice,
        BigDecimal taxRate,
        BigDecimal lineSubtotal,
        BigDecimal lineTax,
        BigDecimal lineTotal
) {
}
