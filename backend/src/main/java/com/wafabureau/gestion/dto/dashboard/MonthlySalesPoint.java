package com.wafabureau.gestion.dto.dashboard;

import java.math.BigDecimal;

public record MonthlySalesPoint(String month, long orderCount, BigDecimal totalAmount) {
}
