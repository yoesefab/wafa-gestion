package com.wafabureau.gestion.controller;
import com.wafabureau.gestion.mapper.*;
import com.wafabureau.gestion.service.*;
import com.wafabureau.gestion.security.AuthenticatedUser;
import com.wafabureau.gestion.enums.*;

import java.net.URI;
import java.time.LocalDate;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wafabureau.gestion.security.AuthenticatedUser;
import com.wafabureau.gestion.dto.purchase.PurchaseOrderDetailResponse;
import com.wafabureau.gestion.dto.purchase.PurchaseOrderSummaryResponse;
import com.wafabureau.gestion.dto.purchase.PurchaseOrderUpdateRequest;
import com.wafabureau.gestion.dto.purchase.PurchaseOrderWriteRequest;
import com.wafabureau.gestion.dto.common.ApiResponse;
import com.wafabureau.gestion.dto.common.PagedResponse;

@RestController
@RequestMapping("/api/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseService;

    public PurchaseOrderController(PurchaseOrderService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseOrderDetailResponse>> create(
            @Valid @RequestBody PurchaseOrderWriteRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        PurchaseOrderDetailResponse order = purchaseService.create(request, user.id());
        return ResponseEntity.created(URI.create("/api/purchase-orders/" + order.id()))
                .body(ApiResponse.of(order));
    }

    @GetMapping
    public PagedResponse<PurchaseOrderSummaryResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "orderDate,desc") String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo
    ) {
        return purchaseService.list(page, size, sort, search, status, supplierId, dateFrom, dateTo);
    }

    @GetMapping("/{id}")
    public ApiResponse<PurchaseOrderDetailResponse> get(@PathVariable Long id) {
        return ApiResponse.of(purchaseService.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<PurchaseOrderDetailResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseOrderUpdateRequest request
    ) {
        return ApiResponse.of(purchaseService.update(id, request));
    }

    @PostMapping("/{id}/order")
    public ApiResponse<PurchaseOrderDetailResponse> markOrdered(@PathVariable Long id) {
        return ApiResponse.of(purchaseService.markOrdered(id));
    }

    @PostMapping("/{id}/receive")
    public ApiResponse<PurchaseOrderDetailResponse> receive(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.of(purchaseService.receive(id, user.id()));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<PurchaseOrderDetailResponse> cancel(@PathVariable Long id) {
        return ApiResponse.of(purchaseService.cancel(id));
    }
}
