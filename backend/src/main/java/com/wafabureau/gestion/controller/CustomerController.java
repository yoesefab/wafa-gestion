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

import com.wafabureau.gestion.dto.customer.CustomerResponse;
import com.wafabureau.gestion.dto.partner.PartnerUpdateRequest;
import com.wafabureau.gestion.dto.partner.PartnerWriteRequest;
import com.wafabureau.gestion.dto.common.ApiResponse;
import com.wafabureau.gestion.dto.common.ArchiveRequest;
import com.wafabureau.gestion.dto.common.PagedResponse;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> create(
            @Valid @RequestBody PartnerWriteRequest request
    ) {
        CustomerResponse customer = customerService.create(request);
        return ResponseEntity.created(URI.create("/api/customers/" + customer.id()))
                .body(ApiResponse.of(customer));
    }

    @GetMapping
    public PagedResponse<CustomerResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name,asc") String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active
    ) {
        return customerService.list(page, size, sort, search, active);
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerResponse> get(@PathVariable Long id) {
        return ApiResponse.of(customerService.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<CustomerResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PartnerUpdateRequest request
    ) {
        return ApiResponse.of(customerService.update(id, request));
    }

    @PostMapping("/{id}/archive")
    public ApiResponse<CustomerResponse> archive(
            @PathVariable Long id,
            @Valid @RequestBody ArchiveRequest request
    ) {
        return ApiResponse.of(customerService.archive(id, request));
    }
}
