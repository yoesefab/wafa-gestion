package com.wafabureau.gestion.model;
import com.wafabureau.gestion.enums.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import com.wafabureau.gestion.model.Customer;
import com.wafabureau.gestion.model.AuditableEntity;
import com.wafabureau.gestion.model.User;

@Entity
@Table(name = "sales_orders")
public class SalesOrder extends AuditableEntity {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, length = 40, unique = true, updatable = false)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SalesOrderStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 1000)
    private String note;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false, updatable = false)
    private User createdBy;

    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SalesOrderItem> items = new ArrayList<>();

    protected SalesOrder() {
    }

    public SalesOrder(String orderNumber, Customer customer, LocalDate orderDate, String note, User createdBy) {
        this.orderNumber = orderNumber;
        this.customer = customer;
        this.orderDate = orderDate;
        this.note = normalizeNote(note);
        this.createdBy = createdBy;
        this.status = SalesOrderStatus.DRAFT;
        this.subtotal = ZERO;
        this.discountAmount = ZERO;
        this.taxAmount = ZERO;
        this.totalAmount = ZERO;
    }

    public void replaceDraft(Customer customer, LocalDate orderDate, String note, List<SalesOrderItemDraft> drafts) {
        this.customer = customer;
        this.orderDate = orderDate;
        this.note = normalizeNote(note);
        Map<Long, SalesOrderItem> existingByProduct = this.items.stream()
                .collect(Collectors.toMap(item -> item.getProduct().getId(), Function.identity()));
        this.items.removeIf(item -> drafts.stream()
                .noneMatch(draft -> draft.product().getId().equals(item.getProduct().getId())));
        drafts.forEach(draft -> {
            SalesOrderItem existing = existingByProduct.get(draft.product().getId());
            if (existing == null) {
                this.items.add(new SalesOrderItem(this, draft.product(), draft.quantity(), draft.taxRate()));
            } else {
                existing.updateDraft(draft.product(), draft.quantity(), draft.taxRate());
            }
        });
        recalculateTotals();
    }

    public void confirm() {
        this.status = SalesOrderStatus.CONFIRMED;
        this.confirmedAt = Instant.now();
    }

    public void cancel() {
        this.status = SalesOrderStatus.CANCELLED;
    }

    public void deliver() {
        this.status = SalesOrderStatus.DELIVERED;
        this.deliveredAt = Instant.now();
    }

    private void recalculateTotals() {
        this.subtotal = items.stream().map(SalesOrderItem::getLineSubtotal).reduce(ZERO, BigDecimal::add);
        this.discountAmount = ZERO;
        this.taxAmount = items.stream().map(SalesOrderItem::getLineTax).reduce(ZERO, BigDecimal::add);
        this.totalAmount = subtotal.subtract(discountAmount).add(taxAmount);
    }

    private String normalizeNote(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public Long getId() { return id; }
    public String getOrderNumber() { return orderNumber; }
    public Customer getCustomer() { return customer; }
    public LocalDate getOrderDate() { return orderDate; }
    public SalesOrderStatus getStatus() { return status; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getNote() { return note; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public User getCreatedBy() { return createdBy; }
    public List<SalesOrderItem> getItems() { return List.copyOf(items); }
}
