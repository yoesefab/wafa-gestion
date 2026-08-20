package com.wafabureau.gestion.controller;
import com.wafabureau.gestion.mapper.*;
import com.wafabureau.gestion.service.*;
import com.wafabureau.gestion.security.AuthenticatedUser;
import com.wafabureau.gestion.enums.*;

import java.net.URI;
import java.time.LocalDate;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import com.wafabureau.gestion.dto.sales.SalesOrderDetailResponse;
import com.wafabureau.gestion.dto.sales.SalesOrderSummaryResponse;
import com.wafabureau.gestion.dto.sales.SalesOrderUpdateRequest;
import com.wafabureau.gestion.dto.sales.SalesOrderWriteRequest;
import com.wafabureau.gestion.dto.common.ApiResponse;
import com.wafabureau.gestion.dto.common.PagedResponse;

@RestController
@RequestMapping("/api/sales-orders")
public class SalesOrderController {
    private final SalesOrderService salesService;
    private final SalesOrderInvoiceService invoiceService;

    public SalesOrderController(SalesOrderService salesService, SalesOrderInvoiceService invoiceService) {
        this.salesService = salesService;
        this.invoiceService = invoiceService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SalesOrderDetailResponse>> create(
            @Valid @RequestBody SalesOrderWriteRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        SalesOrderDetailResponse order = salesService.create(request, user.id());
        return ResponseEntity.created(URI.create("/api/sales-orders/" + order.id())).body(ApiResponse.of(order));
    }

    @GetMapping
    public PagedResponse<SalesOrderSummaryResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "orderDate,desc") String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) SalesOrderStatus status,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo
    ) {
        return salesService.list(page, size, sort, search, status, customerId, dateFrom, dateTo);
    }

    @GetMapping("/{id}")
    public ApiResponse<SalesOrderDetailResponse> get(@PathVariable Long id) {
        return ApiResponse.of(salesService.get(id));
    }

    @GetMapping(value = "/{id}/invoice", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> invoice(@PathVariable Long id) {
        SalesOrderInvoiceService.InvoicePdf invoice = invoiceService.generate(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(invoice.filename()).build().toString())
                .body(invoice.content());
    }

    @PutMapping("/{id}")
    public ApiResponse<SalesOrderDetailResponse> update(
            @PathVariable Long id, @Valid @RequestBody SalesOrderUpdateRequest request
    ) {
        return ApiResponse.of(salesService.update(id, request));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<SalesOrderDetailResponse> confirm(
            @PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.of(salesService.confirm(id, user.id()));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<SalesOrderDetailResponse> cancel(@PathVariable Long id) {
        return ApiResponse.of(salesService.cancel(id));
    }

    @PostMapping("/{id}/deliver")
    public ApiResponse<SalesOrderDetailResponse> deliver(@PathVariable Long id) {
        return ApiResponse.of(salesService.deliver(id));
    }
}
