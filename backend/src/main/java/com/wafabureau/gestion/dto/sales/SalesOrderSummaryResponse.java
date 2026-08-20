package com.wafabureau.gestion.dto.sales;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.wafabureau.gestion.enums.SalesOrderStatus;

public record SalesOrderSummaryResponse(
        Long id,
        String orderNumber,
        SalesPartyResponse party,
        LocalDate orderDate,
        SalesOrderStatus status,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        Long version
) {
}
