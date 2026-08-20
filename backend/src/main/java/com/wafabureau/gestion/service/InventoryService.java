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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wafabureau.gestion.model.Product;
import com.wafabureau.gestion.repository.ProductStockGateway;
import com.wafabureau.gestion.dto.inventory.StockMovementResponse;
import com.wafabureau.gestion.dto.common.PagedResponse;
import com.wafabureau.gestion.exception.BusinessException;
import com.wafabureau.gestion.exception.RequestValidationException;
import com.wafabureau.gestion.exception.ResourceNotFoundException;
import com.wafabureau.gestion.util.PageRequestFactory;
import com.wafabureau.gestion.model.User;
import com.wafabureau.gestion.repository.UserRepository;

@Service
public class InventoryService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Africa/Casablanca");
    private static final Set<String> SORT_FIELDS = Set.of("occurredAt");

    private final ProductStockGateway productStockGateway;
    private final StockMovementRepository stockMovementRepository;
    private final UserRepository userRepository;

    public InventoryService(
            ProductStockGateway productStockGateway,
            StockMovementRepository stockMovementRepository,
            UserRepository userRepository
    ) {
        this.productStockGateway = productStockGateway;
        this.stockMovementRepository = stockMovementRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public StockMovementResponse increaseStock(
            Long productId,
            long quantity,
            String reference,
            String reason,
            String note,
            Long actorId
    ) {
        validatePositiveQuantity(quantity);
        return changeStock(productId, quantity, StockMovementType.STOCK_IN, reference, reason, note, actorId);
    }

    @Transactional
    public StockMovementResponse decreaseStock(
            Long productId,
            long quantity,
            String reference,
            String reason,
            String note,
            Long actorId
    ) {
        validatePositiveQuantity(quantity);
        return changeStock(productId, -quantity, StockMovementType.STOCK_OUT, reference, reason, note, actorId);
    }

    @Transactional
    public StockMovementResponse adjustStock(
            Long productId,
            AdjustmentDirection direction,
            long quantity,
            String reference,
            String reason,
            String note,
            Long actorId
    ) {
        if (direction == null) {
            throw new RequestValidationException("Adjustment direction is required.");
        }
        validatePositiveQuantity(quantity);
        long delta = direction == AdjustmentDirection.IN ? quantity : -quantity;
        return changeStock(productId, delta, StockMovementType.ADJUSTMENT, reference, reason, note, actorId);
    }

    @Transactional
    public StockMovementResponse restoreStock(
            Long productId,
            long quantity,
            String reference,
            String reason,
            String note,
            Long actorId
    ) {
        validatePositiveQuantity(quantity);
        return changeStock(productId, quantity, StockMovementType.RESTORE, reference, reason, note, actorId);
    }

    @Transactional
    public List<StockMovementResponse> decreaseStockForSale(
            List<SaleStockRequest> requests,
            Long actorId
    ) {
        if (requests == null || requests.isEmpty()) {
            throw new RequestValidationException("At least one sale stock request is required.");
        }
        User actor = findActiveUser(actorId);
        HashSet<Long> productIds = new HashSet<>();
        HashSet<Long> lineIds = new HashSet<>();
        for (SaleStockRequest request : requests) {
            if (request == null || request.productId() == null || request.productId() <= 0
                    || request.salesOrderItemId() == null || request.salesOrderItemId() <= 0) {
                throw new RequestValidationException("Product and sales order item identifiers must be positive.");
            }
            validatePositiveQuantity(request.quantity());
            if (request.reference() == null || request.reference().isBlank()
                    || request.reference().trim().length() > 120) {
                throw new RequestValidationException("Reference is required and must not exceed 120 characters.");
            }
            if (!productIds.add(request.productId()) || !lineIds.add(request.salesOrderItemId())) {
                throw new RequestValidationException("A product may appear only once in a sales order.");
            }
        }

        List<Long> sortedProductIds = productIds.stream().sorted().toList();
        List<Product> lockedProducts = productStockGateway.lockAll(sortedProductIds);
        if (lockedProducts.size() != sortedProductIds.size()) {
            throw new ResourceNotFoundException("Product", sortedProductIds.stream()
                    .filter(id -> lockedProducts.stream().noneMatch(product -> product.getId().equals(id)))
                    .findFirst().orElseThrow());
        }
        Map<Long, Product> productsById = new HashMap<>();
        lockedProducts.forEach(product -> productsById.put(product.getId(), product));

        for (SaleStockRequest request : requests) {
            Product product = productsById.get(request.productId());
            if (!product.isActive()) {
                throw new BusinessException("INACTIVE_REFERENCE", "Stock cannot be changed for an inactive product.");
            }
            if (product.getCurrentStock() < request.quantity()) {
                throw new BusinessException("INSUFFICIENT_STOCK", "The product does not have enough stock.");
            }
        }

        Map<Long, SaleStockRequest> requestsByProduct = new HashMap<>();
        requests.forEach(request -> requestsByProduct.put(request.productId(), request));
        return lockedProducts.stream().map(product -> {
            SaleStockRequest request = requestsByProduct.get(product.getId());
            long stockBefore = product.getCurrentStock();
            long stockAfter = stockBefore - request.quantity();
            productStockGateway.replaceStock(product, stockAfter);
            StockMovement movement = new StockMovement(
                    product,
                    StockMovementType.STOCK_OUT,
                    -request.quantity(),
                    stockBefore,
                    stockAfter,
                    request.reference(),
                    "Sales order confirmation",
                    null,
                    request.salesOrderItemId(),
                    null,
                    actor
            );
            return StockMovementMapper.toResponse(stockMovementRepository.save(movement));
        }).toList();
    }

    @Transactional
    public List<StockMovementResponse> increaseStockForPurchase(
            List<PurchaseStockRequest> requests,
            Long actorId
    ) {
        if (requests == null || requests.isEmpty()) {
            throw new RequestValidationException("At least one purchase stock request is required.");
        }
        User actor = findActiveUser(actorId);
        HashSet<Long> productIds = new HashSet<>();
        HashSet<Long> lineIds = new HashSet<>();
        for (PurchaseStockRequest request : requests) {
            if (request == null || request.productId() == null || request.productId() <= 0
                    || request.purchaseOrderItemId() == null || request.purchaseOrderItemId() <= 0) {
                throw new RequestValidationException("Product and purchase order item identifiers must be positive.");
            }
            validatePositiveQuantity(request.quantity());
            if (request.reference() == null || request.reference().isBlank()
                    || request.reference().trim().length() > 120) {
                throw new RequestValidationException("Reference is required and must not exceed 120 characters.");
            }
            if (!productIds.add(request.productId()) || !lineIds.add(request.purchaseOrderItemId())) {
                throw new RequestValidationException("A product may appear only once in a purchase order.");
            }
        }

        List<Long> sortedProductIds = productIds.stream().sorted().toList();
        List<Product> lockedProducts = productStockGateway.lockAll(sortedProductIds);
        if (lockedProducts.size() != sortedProductIds.size()) {
            throw new ResourceNotFoundException("Product", sortedProductIds.stream()
                    .filter(id -> lockedProducts.stream().noneMatch(product -> product.getId().equals(id)))
                    .findFirst().orElseThrow());
        }
        Map<Long, Product> productsById = new HashMap<>();
        lockedProducts.forEach(product -> productsById.put(product.getId(), product));

        Map<Long, Long> resultingStock = new HashMap<>();
        for (PurchaseStockRequest request : requests) {
            Product product = productsById.get(request.productId());
            if (!product.isActive()) {
                throw new BusinessException("INACTIVE_REFERENCE", "Stock cannot be changed for an inactive product.");
            }
            try {
                resultingStock.put(product.getId(), Math.addExact(product.getCurrentStock(), request.quantity()));
            } catch (ArithmeticException exception) {
                throw new BusinessException("STOCK_LIMIT_EXCEEDED", "The stock operation exceeds the supported range.");
            }
        }

        Map<Long, PurchaseStockRequest> requestsByProduct = new HashMap<>();
        requests.forEach(request -> requestsByProduct.put(request.productId(), request));
        return lockedProducts.stream().map(product -> {
            PurchaseStockRequest request = requestsByProduct.get(product.getId());
            long stockBefore = product.getCurrentStock();
            long stockAfter = resultingStock.get(product.getId());
            productStockGateway.replaceStock(product, stockAfter);
            StockMovement movement = new StockMovement(
                    product,
                    StockMovementType.STOCK_IN,
                    request.quantity(),
                    stockBefore,
                    stockAfter,
                    request.reference(),
                    "Purchase order receipt",
                    null,
                    null,
                    request.purchaseOrderItemId(),
                    actor
            );
            return StockMovementMapper.toResponse(stockMovementRepository.save(movement));
        }).toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<StockMovementResponse> list(
            int page,
            int size,
            String sort,
            Long productId,
            StockMovementType type,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        validateFilters(productId, dateFrom, dateTo);
        Page<StockMovement> movements = stockMovementRepository.findAll(
                StockMovementSpecifications.matches(
                        productId,
                        type,
                        dateFrom == null ? null : dateFrom.atStartOfDay(BUSINESS_ZONE).toInstant(),
                        dateTo == null ? null : dateTo.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant()
                ),
                PageRequestFactory.create(page, size, sort, SORT_FIELDS)
        );
        return PageMapper.toResponse(movements, StockMovementMapper::toResponse);
    }

    private StockMovementResponse changeStock(
            Long productId,
            long delta,
            StockMovementType type,
            String reference,
            String reason,
            String note,
            Long actorId
    ) {
        validateText(reference, reason, note);
        if (productId == null || productId <= 0 || actorId == null || actorId <= 0) {
            throw new RequestValidationException("Product and actor identifiers must be positive.");
        }
        User actor = findActiveUser(actorId);
        Product product = productStockGateway.lock(productId);
        if (!product.isActive()) {
            throw new BusinessException("INACTIVE_REFERENCE", "Stock cannot be changed for an inactive product.");
        }

        long stockBefore = product.getCurrentStock();
        long stockAfter;
        try {
            stockAfter = Math.addExact(stockBefore, delta);
        } catch (ArithmeticException exception) {
            throw new BusinessException("STOCK_LIMIT_EXCEEDED", "The stock operation exceeds the supported range.");
        }
        if (stockAfter < 0) {
            throw new BusinessException("INSUFFICIENT_STOCK", "The product does not have enough stock.");
        }

        productStockGateway.replaceStock(product, stockAfter);
        StockMovement movement = new StockMovement(
                product,
                type,
                delta,
                stockBefore,
                stockAfter,
                reference,
                reason,
                note,
                null,
                null,
                actor
        );
        return StockMovementMapper.toResponse(stockMovementRepository.saveAndFlush(movement));
    }

    private User findActiveUser(Long actorId) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", actorId));
        if (!actor.isActive()) {
            throw new BusinessException("INACTIVE_REFERENCE", "The user recording the movement is inactive.");
        }
        return actor;
    }

    private void validatePositiveQuantity(long quantity) {
        if (quantity <= 0) {
            throw new RequestValidationException("Quantity must be greater than zero.");
        }
    }

    private void validateText(String reference, String reason, String note) {
        if (reference == null || reference.isBlank() || reference.trim().length() > 120) {
            throw new RequestValidationException("Reference is required and must not exceed 120 characters.");
        }
        if (reason == null || reason.isBlank() || reason.trim().length() > 180) {
            throw new RequestValidationException("Reason is required and must not exceed 180 characters.");
        }
        if (note != null && note.trim().length() > 1000) {
            throw new RequestValidationException("Note must not exceed 1000 characters.");
        }
    }

    private void validateFilters(Long productId, LocalDate dateFrom, LocalDate dateTo) {
        if (productId != null && productId <= 0) {
            throw new RequestValidationException("Product identifier must be positive.");
        }
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new RequestValidationException("dateFrom must be before or equal to dateTo.");
        }
    }
}
