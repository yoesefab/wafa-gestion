package com.wafabureau.gestion.dto.sales;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.wafabureau.gestion.enums.SalesOrderStatus;

public record SalesOrderDetailResponse(
        Long id,
        String orderNumber,
        SalesPartyResponse customer,
        LocalDate orderDate,
        SalesOrderStatus status,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String note,
        Instant confirmedAt,
        Instant deliveredAt,
        SalesActorResponse createdBy,
        Instant createdAt,
        Instant updatedAt,
        Long version,
        List<SalesOrderItemResponse> lines
) {
}
