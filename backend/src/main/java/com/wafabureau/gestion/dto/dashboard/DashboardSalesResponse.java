package com.wafabureau.gestion.dto.dashboard;

import java.util.List;

public record DashboardSalesResponse(int year, String currency, List<MonthlySalesPoint> months) {
}
