package com.wafabureau.gestion.controller;
import com.wafabureau.gestion.mapper.*;
import com.wafabureau.gestion.service.*;
import com.wafabureau.gestion.security.AuthenticatedUser;
import com.wafabureau.gestion.enums.*;

import java.net.URI;
import java.time.LocalDate;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wafabureau.gestion.security.AuthenticatedUser;
import com.wafabureau.gestion.dto.inventory.ManualStockAdjustmentRequest;
import com.wafabureau.gestion.dto.inventory.StockMovementResponse;
import com.wafabureau.gestion.dto.common.ApiResponse;
import com.wafabureau.gestion.dto.common.PagedResponse;

@RestController
@RequestMapping("/api")
public class StockMovementController {

    private final InventoryService inventoryService;

    public StockMovementController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/stock-movements")
    public PagedResponse<StockMovementResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "occurredAt,desc") String sort,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) StockMovementType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return inventoryService.list(page, size, sort, productId, type, dateFrom, dateTo);
    }

    @PostMapping("/products/{id}/stock-adjustment")
    public ResponseEntity<ApiResponse<StockMovementResponse>> adjust(
            @PathVariable Long id,
            @Valid @RequestBody ManualStockAdjustmentRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        StockMovementResponse movement = inventoryService.adjustStock(
                id,
                request.direction(),
                request.quantity(),
                request.reference(),
                request.reason(),
                request.note(),
                user.id()
        );
        return ResponseEntity.created(URI.create("/api/stock-movements/" + movement.id()))
                .body(ApiResponse.of(movement));
    }
}
