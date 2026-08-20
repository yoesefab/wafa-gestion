package com.wafabureau.gestion.service;
import com.wafabureau.gestion.mapper.*;
import com.wafabureau.gestion.exception.*;
import com.wafabureau.gestion.model.*;
import com.wafabureau.gestion.repository.*;
import com.wafabureau.gestion.repository.specification.*;
import com.wafabureau.gestion.dto.auth.*;
import com.wafabureau.gestion.dto.category.*;
import com.wafabureau.gestion.dto.product.*;
import com.wafabureau.gestion.dto.customer.*;
import com.wafabureau.gestion.dto.supplier.*;
import com.wafabureau.gestion.dto.partner.*;
import com.wafabureau.gestion.dto.inventory.*;
import com.wafabureau.gestion.dto.sales.*;
import com.wafabureau.gestion.dto.purchase.*;
import com.wafabureau.gestion.enums.*;
import com.wafabureau.gestion.security.*;

import java.util.Locale;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wafabureau.gestion.dto.product.ProductResponse;
import com.wafabureau.gestion.dto.product.ProductUpdateRequest;
import com.wafabureau.gestion.dto.product.ProductWriteRequest;
import com.wafabureau.gestion.dto.common.PagedResponse;
import com.wafabureau.gestion.dto.common.ArchiveRequest;
import com.wafabureau.gestion.exception.BusinessException;
import com.wafabureau.gestion.exception.RequestValidationException;
import com.wafabureau.gestion.exception.ResourceNotFoundException;
import com.wafabureau.gestion.util.PageRequestFactory;

@Service
public class ProductService {

    private static final Set<String> SORT_FIELDS = Set.of("name", "sku", "currentStock", "createdAt");

    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    public ProductService(ProductRepository productRepository, CategoryService categoryService) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
    }

    @Transactional
    public ProductResponse create(ProductWriteRequest request) {
        ensureSkuAvailable(normalizeSku(request.sku()), null);
        Category category = categoryService.findActive(request.categoryId());
        Product product = new Product(
                request.sku(),
                request.name(),
                category,
                request.unitOfMeasure(),
                request.purchasePrice(),
                request.sellingPrice(),
                request.minimumStock()
        );
        try {
            return ProductMapper.toResponse(productRepository.saveAndFlush(product));
        } catch (DataIntegrityViolationException exception) {
            throw duplicateSku();
        }
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> list(
            int page,
            int size,
            String sort,
            String search,
            Long categoryId,
            Boolean active,
            Boolean lowStock
    ) {
        validateFilters(search, categoryId);
        Page<Product> products = productRepository.findAll(
                ProductSpecifications.matches(search, categoryId, active, lowStock),
                PageRequestFactory.create(page, size, sort, SORT_FIELDS)
        );
        return PageMapper.toResponse(products, ProductMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> listActiveLowStock(int page, int size, String sort) {
        return list(page, size, sort, null, null, true, true);
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return ProductMapper.toResponse(find(id));
    }

    @Transactional
    public ProductResponse update(Long id, ProductUpdateRequest request) {
        Product product = find(id);
        verifyVersion(product.getVersion(), request.version());
        ensureSkuAvailable(normalizeSku(request.sku()), id);
        Category category = categoryService.findActive(request.categoryId());
        product.update(
                request.sku(),
                request.name(),
                category,
                request.unitOfMeasure(),
                request.purchasePrice(),
                request.sellingPrice(),
                request.minimumStock()
        );
        try {
            return ProductMapper.toResponse(productRepository.saveAndFlush(product));
        } catch (DataIntegrityViolationException exception) {
            throw duplicateSku();
        }
    }

    @Transactional
    public ProductResponse archive(Long id, ArchiveRequest request) {
        Product product = find(id);
        verifyVersion(product.getVersion(), request.version());
        if (!product.isActive()) {
            throw new BusinessException("INVALID_STATE_TRANSITION", "The product is already archived.");
        }
        product.archive();
        return ProductMapper.toResponse(productRepository.saveAndFlush(product));
    }

    private Product find(Long id) {
        return productRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    private void ensureSkuAvailable(String normalizedSku, Long excludedId) {
        boolean exists = excludedId == null
                ? productRepository.existsByNormalizedSku(normalizedSku)
                : productRepository.existsByNormalizedSkuAndIdNot(normalizedSku, excludedId);
        if (exists) {
            throw duplicateSku();
        }
    }

    private String normalizeSku(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private BusinessException duplicateSku() {
        return new BusinessException("DUPLICATE_RESOURCE", "A product with this reference already exists.");
    }

    private void validateFilters(String search, Long categoryId) {
        if (search != null && search.trim().length() > 180) {
            throw new RequestValidationException("Product search must not exceed 180 characters.");
        }
        if (categoryId != null && categoryId <= 0) {
            throw new RequestValidationException("Category identifier must be positive.");
        }
    }

    private void verifyVersion(Long actual, Long expected) {
        if (!actual.equals(expected)) {
            throw new BusinessException("VERSION_CONFLICT", "The product was modified by another request.");
        }
    }
}
