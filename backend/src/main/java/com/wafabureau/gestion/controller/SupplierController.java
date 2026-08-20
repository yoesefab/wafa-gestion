package com.wafabureau.gestion.controller;
import com.wafabureau.gestion.mapper.*;
import com.wafabureau.gestion.service.*;
import com.wafabureau.gestion.security.AuthenticatedUser;
import com.wafabureau.gestion.enums.*;

import java.net.URI;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wafabureau.gestion.dto.partner.PartnerUpdateRequest;
import com.wafabureau.gestion.dto.partner.PartnerWriteRequest;
import com.wafabureau.gestion.dto.supplier.SupplierResponse;
import com.wafabureau.gestion.dto.common.ApiResponse;
import com.wafabureau.gestion.dto.common.ArchiveRequest;
import com.wafabureau.gestion.dto.common.PagedResponse;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SupplierResponse>> create(
            @Valid @RequestBody PartnerWriteRequest request
    ) {
        SupplierResponse supplier = supplierService.create(request);
        return ResponseEntity.created(URI.create("/api/suppliers/" + supplier.id()))
                .body(ApiResponse.of(supplier));
    }

    @GetMapping
    public PagedResponse<SupplierResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name,asc") String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active
    ) {
        return supplierService.list(page, size, sort, search, active);
    }

    @GetMapping("/{id}")
    public ApiResponse<SupplierResponse> get(@PathVariable Long id) {
        return ApiResponse.of(supplierService.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<SupplierResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PartnerUpdateRequest request
    ) {
        return ApiResponse.of(supplierService.update(id, request));
    }

    @PostMapping("/{id}/archive")
    public ApiResponse<SupplierResponse> archive(
            @PathVariable Long id,
            @Valid @RequestBody ArchiveRequest request
    ) {
        return ApiResponse.of(supplierService.archive(id, request));
    }
}
