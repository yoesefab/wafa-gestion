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

import com.wafabureau.gestion.dto.category.CategoryResponse;
import com.wafabureau.gestion.dto.category.CategoryUpdateRequest;
import com.wafabureau.gestion.dto.category.CategoryWriteRequest;
import com.wafabureau.gestion.dto.common.ApiResponse;
import com.wafabureau.gestion.dto.common.ArchiveRequest;
import com.wafabureau.gestion.dto.common.PagedResponse;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @Valid @RequestBody CategoryWriteRequest request
    ) {
        CategoryResponse category = categoryService.create(request);
        return ResponseEntity.created(URI.create("/api/categories/" + category.id()))
                .body(ApiResponse.of(category));
    }

    @GetMapping
    public PagedResponse<CategoryResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name,asc") String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active
    ) {
        return categoryService.list(page, size, sort, search, active);
    }

    @GetMapping("/{id}")
    public ApiResponse<CategoryResponse> get(@PathVariable Long id) {
        return ApiResponse.of(categoryService.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoryUpdateRequest request
    ) {
        return ApiResponse.of(categoryService.update(id, request));
    }

    @PostMapping("/{id}/archive")
    public ApiResponse<CategoryResponse> archive(
            @PathVariable Long id,
            @Valid @RequestBody ArchiveRequest request
    ) {
        return ApiResponse.of(categoryService.archive(id, request));
    }
}
