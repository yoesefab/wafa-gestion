package com.wafabureau.gestion.dto.purchase;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.wafabureau.gestion.enums.PurchaseOrderStatus;

public record PurchaseOrderSummaryResponse(
        Long id,
        String orderNumber,
        PurchasePartyResponse party,
        LocalDate orderDate,
        PurchaseOrderStatus status,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        Long version
) {
}
