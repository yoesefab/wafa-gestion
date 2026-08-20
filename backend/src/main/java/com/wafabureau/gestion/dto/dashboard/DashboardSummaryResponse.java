package com.wafabureau.gestion.dto.dashboard;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        BigDecimal revenueThisMonth,
        long salesOrderCountThisMonth,
        long totalActiveProducts,
        long lowStockProductCount
) {
}
