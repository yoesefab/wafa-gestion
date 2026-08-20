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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wafabureau.gestion.model.Product;
import com.wafabureau.gestion.service.ProductSelector;
import com.wafabureau.gestion.service.InventoryService;
import com.wafabureau.gestion.dto.inventory.PurchaseStockRequest;
import com.wafabureau.gestion.model.Supplier;
import com.wafabureau.gestion.service.SupplierSelector;
import com.wafabureau.gestion.dto.purchase.PurchaseOrderDetailResponse;
import com.wafabureau.gestion.dto.purchase.PurchaseOrderLineWriteRequest;
import com.wafabureau.gestion.dto.purchase.PurchaseOrderSummaryResponse;
import com.wafabureau.gestion.dto.purchase.PurchaseOrderUpdateRequest;
import com.wafabureau.gestion.dto.purchase.PurchaseOrderWriteRequest;
import com.wafabureau.gestion.dto.common.PagedResponse;
import com.wafabureau.gestion.exception.BusinessException;
import com.wafabureau.gestion.exception.RequestValidationException;
import com.wafabureau.gestion.exception.ResourceNotFoundException;
import com.wafabureau.gestion.util.PageRequestFactory;
import com.wafabureau.gestion.model.User;
import com.wafabureau.gestion.repository.UserRepository;

@Service
public class PurchaseOrderService {

    private static final Set<String> SORT_FIELDS = Set.of("orderDate", "orderNumber", "totalAmount", "status");

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierSelector supplierSelector;
    private final ProductSelector productSelector;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;

    public PurchaseOrderService(
            PurchaseOrderRepository purchaseOrderRepository,
            SupplierSelector supplierSelector,
            ProductSelector productSelector,
            UserRepository userRepository,
            InventoryService inventoryService
    ) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.supplierSelector = supplierSelector;
        this.productSelector = productSelector;
        this.userRepository = userRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public PurchaseOrderDetailResponse create(PurchaseOrderWriteRequest request, Long actorId) {
        Supplier supplier = requireActiveSupplier(request.supplierId());
        User actor = requireActiveUser(actorId);
        String orderNumber = "PO-%d-%06d".formatted(
                request.orderDate().getYear(), purchaseOrderRepository.nextOrderNumberSequence());
        PurchaseOrder order = new PurchaseOrder(orderNumber, supplier, request.orderDate(), request.note(), actor);
        order.replaceDraft(supplier, request.orderDate(), request.note(), buildDrafts(request.lines()));
        return PurchaseOrderMapper.toDetail(purchaseOrderRepository.saveAndFlush(order));
    }

    @Transactional
    public PurchaseOrderDetailResponse update(Long id, PurchaseOrderUpdateRequest request) {
        PurchaseOrder order = requireForUpdate(id);
        requireStatus(order, PurchaseOrderStatus.DRAFT, "Only a draft purchase order can be updated.");
        if (!order.getVersion().equals(request.version())) {
            throw new BusinessException("VERSION_CONFLICT", "The purchase order was modified by another request.");
        }
        Supplier supplier = requireActiveSupplier(request.supplierId());
        order.replaceDraft(supplier, request.orderDate(), request.note(), buildDrafts(request.lines()));
        purchaseOrderRepository.flush();
        return PurchaseOrderMapper.toDetail(order);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PurchaseOrderSummaryResponse> list(
            int page, int size, String sort, String search, PurchaseOrderStatus status,
            Long supplierId, LocalDate dateFrom, LocalDate dateTo
    ) {
        validateFilters(search, supplierId, dateFrom, dateTo);
        Page<PurchaseOrder> orders = purchaseOrderRepository.findAll(
                PurchaseOrderSpecifications.matches(search, status, supplierId, dateFrom, dateTo),
                PageRequestFactory.create(page, size, sort, SORT_FIELDS)
        );
        return PageMapper.toResponse(orders, PurchaseOrderMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public PurchaseOrderDetailResponse get(Long id) {
        return PurchaseOrderMapper.toDetail(requireDetailed(id));
    }

    @Transactional
    public PurchaseOrderDetailResponse markOrdered(Long id) {
        PurchaseOrder order = requireForUpdate(id);
        requireStatus(order, PurchaseOrderStatus.DRAFT, "Only a draft purchase order can be marked ordered.");
        validateActiveReferences(order);
        order.markOrdered();
        purchaseOrderRepository.flush();
        return PurchaseOrderMapper.toDetail(order);
    }

    @Transactional
    public PurchaseOrderDetailResponse receive(Long id, Long actorId) {
        PurchaseOrder order = requireForUpdate(id);
        requireStatus(order, PurchaseOrderStatus.ORDERED, "Only an ordered purchase order can be received.");
        validateActiveReferences(order);
        List<PurchaseStockRequest> stockRequests = order.getItems().stream()
                .map(item -> new PurchaseStockRequest(
                        item.getProduct().getId(), item.getId(), item.getQuantity(), order.getOrderNumber()))
                .toList();
        inventoryService.increaseStockForPurchase(stockRequests, actorId);
        order.receive();
        purchaseOrderRepository.flush();
        return PurchaseOrderMapper.toDetail(order);
    }

    @Transactional
    public PurchaseOrderDetailResponse cancel(Long id) {
        PurchaseOrder order = requireForUpdate(id);
        if (order.getStatus() != PurchaseOrderStatus.DRAFT && order.getStatus() != PurchaseOrderStatus.ORDERED) {
            throw new BusinessException(
                    "INVALID_STATE_TRANSITION", "Only a draft or ordered purchase order can be cancelled.");
        }
        order.cancel();
        purchaseOrderRepository.flush();
        return PurchaseOrderMapper.toDetail(order);
    }

    private List<PurchaseOrderItemDraft> buildDrafts(List<PurchaseOrderLineWriteRequest> lines) {
        HashSet<Long> productIds = new HashSet<>();
        return lines.stream().map(line -> {
            if (line == null || line.productId() == null || line.productId() <= 0
                    || line.quantity() == null || line.quantity() <= 0
                    || line.unitPrice() == null || line.unitPrice().compareTo(BigDecimal.ZERO) < 0
                    || line.taxRate() == null || line.taxRate().compareTo(BigDecimal.ZERO) < 0
                    || line.taxRate().compareTo(new BigDecimal("100.00")) > 0) {
                throw new RequestValidationException(
                        "Each order item requires a product, positive quantity, non-negative price, and tax rate from 0 to 100."
                );
            }
            if (!productIds.add(line.productId())) {
                throw new BusinessException("DUPLICATE_ORDER_ITEM", "A product may appear only once in a purchase order.");
            }
            Product product = productSelector.require(line.productId());
            if (!product.isActive()) {
                throw new BusinessException("INACTIVE_REFERENCE", "An inactive product cannot be added to an order.");
            }
            return new PurchaseOrderItemDraft(product, line.quantity(), line.unitPrice(), line.taxRate());
        }).toList();
    }

    private void validateActiveReferences(PurchaseOrder order) {
        requireActiveSupplier(order.getSupplier().getId());
        if (order.getItems().isEmpty()) {
            throw new BusinessException("INVALID_ORDER", "A purchase order must contain at least one item.");
        }
        if (order.getItems().stream().anyMatch(item -> !item.getProduct().isActive())) {
            throw new BusinessException("INACTIVE_REFERENCE", "An inactive product cannot be ordered or received.");
        }
    }

    private Supplier requireActiveSupplier(Long id) {
        Supplier supplier = supplierSelector.require(id);
        if (!supplier.isActive()) {
            throw new BusinessException("INACTIVE_REFERENCE", "An inactive supplier cannot be used for a purchase order.");
        }
        return supplier;
    }

    private User requireActiveUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (!user.isActive()) {
            throw new BusinessException("INACTIVE_REFERENCE", "The user creating the order is inactive.");
        }
        return user;
    }

    private PurchaseOrder requireDetailed(Long id) {
        return purchaseOrderRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order", id));
    }

    private PurchaseOrder requireForUpdate(Long id) {
        return purchaseOrderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order", id));
    }

    private void requireStatus(PurchaseOrder order, PurchaseOrderStatus required, String message) {
        if (order.getStatus() != required) {
            throw new BusinessException("INVALID_STATE_TRANSITION", message);
        }
    }

    private void validateFilters(String search, Long supplierId, LocalDate dateFrom, LocalDate dateTo) {
        if (search != null && search.trim().length() > 180) {
            throw new RequestValidationException("Search must not exceed 180 characters.");
        }
        if (supplierId != null && supplierId <= 0) {
            throw new RequestValidationException("Supplier identifier must be positive.");
        }
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new RequestValidationException("dateFrom must be before or equal to dateTo.");
        }
    }
}
