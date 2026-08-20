package com.wafabureau.gestion.dto.dashboard;

import java.math.BigDecimal;

public record TopProductResponse(
        DashboardProductReference product,
        long quantitySold,
        BigDecimal revenue
) {
}
