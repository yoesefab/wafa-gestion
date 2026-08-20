package com.wafabureau.gestion.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wafabureau.gestion.dto.common.ApiResponse;
import com.wafabureau.gestion.dto.dashboard.DashboardSalesResponse;
import com.wafabureau.gestion.dto.dashboard.DashboardSummaryResponse;
import com.wafabureau.gestion.dto.dashboard.LowStockProductResponse;
import com.wafabureau.gestion.dto.dashboard.TopProductResponse;
import com.wafabureau.gestion.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> summary() {
        return ApiResponse.of(dashboardService.summary());
    }

    @GetMapping("/sales")
    public ApiResponse<DashboardSalesResponse> sales(@RequestParam(required = false) Integer year) {
        return ApiResponse.of(dashboardService.sales(year));
    }

    @GetMapping("/top-products")
    public ApiResponse<List<TopProductResponse>> topProducts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.of(dashboardService.topProducts(dateFrom, dateTo, limit));
    }

    @GetMapping("/low-stock")
    public ApiResponse<List<LowStockProductResponse>> lowStock(
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.of(dashboardService.lowStock(limit));
    }
}
