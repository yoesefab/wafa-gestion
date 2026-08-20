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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wafabureau.gestion.model.Product;
import com.wafabureau.gestion.service.ProductSelector;
import com.wafabureau.gestion.service.InventoryService;
import com.wafabureau.gestion.dto.inventory.SaleStockRequest;
import com.wafabureau.gestion.model.Customer;
import com.wafabureau.gestion.service.CustomerSelector;
import com.wafabureau.gestion.dto.sales.SalesOrderDetailResponse;
import com.wafabureau.gestion.dto.sales.SalesOrderLineWriteRequest;
import com.wafabureau.gestion.dto.sales.SalesOrderSummaryResponse;
import com.wafabureau.gestion.dto.sales.SalesOrderUpdateRequest;
import com.wafabureau.gestion.dto.sales.SalesOrderWriteRequest;
import com.wafabureau.gestion.dto.common.PagedResponse;
import com.wafabureau.gestion.exception.BusinessException;
import com.wafabureau.gestion.exception.RequestValidationException;
import com.wafabureau.gestion.exception.ResourceNotFoundException;
import com.wafabureau.gestion.util.PageRequestFactory;
import com.wafabureau.gestion.model.User;
import com.wafabureau.gestion.repository.UserRepository;

@Service
public class SalesOrderService {

    private static final Set<String> SORT_FIELDS = Set.of("orderDate", "orderNumber", "totalAmount", "status");

    private final SalesOrderRepository salesOrderRepository;
    private final CustomerSelector customerSelector;
    private final ProductSelector productSelector;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;

    public SalesOrderService(
            SalesOrderRepository salesOrderRepository,
            CustomerSelector customerSelector,
            ProductSelector productSelector,
            UserRepository userRepository,
            InventoryService inventoryService
    ) {
        this.salesOrderRepository = salesOrderRepository;
        this.customerSelector = customerSelector;
        this.productSelector = productSelector;
        this.userRepository = userRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public SalesOrderDetailResponse create(SalesOrderWriteRequest request, Long actorId) {
        Customer customer = requireActiveCustomer(request.customerId());
        User actor = requireActiveUser(actorId);
        String orderNumber = "SO-%d-%06d".formatted(
                request.orderDate().getYear(), salesOrderRepository.nextOrderNumberSequence());
        SalesOrder order = new SalesOrder(orderNumber, customer, request.orderDate(), request.note(), actor);
        order.replaceDraft(customer, request.orderDate(), request.note(), buildDrafts(request.lines()));
        return SalesOrderMapper.toDetail(salesOrderRepository.saveAndFlush(order));
    }

    @Transactional
    public SalesOrderDetailResponse update(Long id, SalesOrderUpdateRequest request) {
        SalesOrder order = requireDetailedForUpdate(id);
        requireStatus(order, SalesOrderStatus.DRAFT, "Only a draft sales order can be updated.");
        if (!order.getVersion().equals(request.version())) {
            throw new BusinessException("VERSION_CONFLICT", "The sales order was modified by another request.");
        }
        Customer customer = requireActiveCustomer(request.customerId());
        order.replaceDraft(customer, request.orderDate(), request.note(), buildDrafts(request.lines()));
        salesOrderRepository.flush();
        return SalesOrderMapper.toDetail(order);
    }

    @Transactional(readOnly = true)
    public PagedResponse<SalesOrderSummaryResponse> list(
            int page, int size, String sort, String search, SalesOrderStatus status,
            Long customerId, LocalDate dateFrom, LocalDate dateTo
    ) {
        validateFilters(search, customerId, dateFrom, dateTo);
        Page<SalesOrder> orders = salesOrderRepository.findAll(
                SalesOrderSpecifications.matches(search, status, customerId, dateFrom, dateTo),
                PageRequestFactory.create(page, size, sort, SORT_FIELDS)
        );
        return PageMapper.toResponse(orders, SalesOrderMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public SalesOrderDetailResponse get(Long id) {
        return SalesOrderMapper.toDetail(requireDetailed(id));
    }

    @Transactional
    public SalesOrderDetailResponse confirm(Long id, Long actorId) {
        SalesOrder order = requireDetailedForUpdate(id);
        requireStatus(order, SalesOrderStatus.DRAFT, "Only a draft sales order can be confirmed.");
        requireActiveCustomer(order.getCustomer().getId());
        if (order.getItems().isEmpty()) {
            throw new BusinessException("INVALID_ORDER", "A sales order must contain at least one item.");
        }
        List<SaleStockRequest> stockRequests = order.getItems().stream()
                .map(item -> new SaleStockRequest(
                        item.getProduct().getId(), item.getId(), item.getQuantity(), order.getOrderNumber()))
                .toList();
        inventoryService.decreaseStockForSale(stockRequests, actorId);
        order.confirm();
        salesOrderRepository.flush();
        return SalesOrderMapper.toDetail(order);
    }

    @Transactional
    public SalesOrderDetailResponse cancel(Long id) {
        SalesOrder order = requireDetailedForUpdate(id);
        requireStatus(order, SalesOrderStatus.DRAFT, "Only a draft sales order can be cancelled.");
        order.cancel();
        salesOrderRepository.flush();
        return SalesOrderMapper.toDetail(order);
    }

    @Transactional
    public SalesOrderDetailResponse deliver(Long id) {
        SalesOrder order = requireDetailedForUpdate(id);
        requireStatus(order, SalesOrderStatus.CONFIRMED, "Only a confirmed sales order can be delivered.");
        order.deliver();
        salesOrderRepository.flush();
        return SalesOrderMapper.toDetail(order);
    }

    private List<SalesOrderItemDraft> buildDrafts(List<SalesOrderLineWriteRequest> lines) {
        HashSet<Long> productIds = new HashSet<>();
        return lines.stream().map(line -> {
            if (line == null || line.productId() == null || line.productId() <= 0
                    || line.quantity() == null || line.quantity() <= 0
                    || line.taxRate() == null || line.taxRate().compareTo(BigDecimal.ZERO) < 0
                    || line.taxRate().compareTo(new BigDecimal("100.00")) > 0) {
                throw new RequestValidationException(
                        "Each order item requires a product, a positive quantity, and a tax rate between 0 and 100."
                );
            }
            if (!productIds.add(line.productId())) {
                throw new BusinessException("DUPLICATE_ORDER_ITEM", "A product may appear only once in a sales order.");
            }
            Product product = productSelector.require(line.productId());
            if (!product.isActive()) {
                throw new BusinessException("INACTIVE_REFERENCE", "An inactive product cannot be added to an order.");
            }
            return new SalesOrderItemDraft(product, line.quantity(), line.taxRate());
        }).toList();
    }

    private Customer requireActiveCustomer(Long id) {
        Customer customer = customerSelector.require(id);
        if (!customer.isActive()) {
            throw new BusinessException("INACTIVE_REFERENCE", "An inactive customer cannot be used for a sales order.");
        }
        return customer;
    }

    private User requireActiveUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (!user.isActive()) {
            throw new BusinessException("INACTIVE_REFERENCE", "The user creating the order is inactive.");
        }
        return user;
    }

    private SalesOrder requireDetailed(Long id) {
        return salesOrderRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales order", id));
    }

    private SalesOrder requireDetailedForUpdate(Long id) {
        return salesOrderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales order", id));
    }

    private void requireStatus(SalesOrder order, SalesOrderStatus required, String message) {
        if (order.getStatus() != required) {
            throw new BusinessException("INVALID_STATE_TRANSITION", message);
        }
    }

    private void validateFilters(String search, Long customerId, LocalDate dateFrom, LocalDate dateTo) {
        if (search != null && search.trim().length() > 180) {
            throw new RequestValidationException("Search must not exceed 180 characters.");
        }
        if (customerId != null && customerId <= 0) {
            throw new RequestValidationException("Customer identifier must be positive.");
        }
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new RequestValidationException("dateFrom must be before or equal to dateTo.");
        }
    }
}
