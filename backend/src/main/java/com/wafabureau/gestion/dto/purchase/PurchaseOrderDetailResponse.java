package com.wafabureau.gestion.dto.purchase;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.wafabureau.gestion.enums.PurchaseOrderStatus;

public record PurchaseOrderDetailResponse(
        Long id,
        String orderNumber,
        PurchasePartyResponse supplier,
        LocalDate orderDate,
        PurchaseOrderStatus status,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String note,
        Instant orderedAt,
        Instant receivedAt,
        PurchaseActorResponse createdBy,
        Instant createdAt,
        Instant updatedAt,
        Long version,
        List<PurchaseOrderItemResponse> lines
) {
}
