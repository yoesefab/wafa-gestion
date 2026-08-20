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

import com.wafabureau.gestion.model.Supplier;
import com.wafabureau.gestion.model.AuditableEntity;
import com.wafabureau.gestion.model.User;

@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder extends AuditableEntity {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, length = 40, unique = true, updatable = false)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PurchaseOrderStatus status;

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

    @Column(name = "ordered_at")
    private Instant orderedAt;

    @Column(name = "received_at")
    private Instant receivedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false, updatable = false)
    private User createdBy;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrderItem> items = new ArrayList<>();

    protected PurchaseOrder() {
    }

    public PurchaseOrder(String orderNumber, Supplier supplier, LocalDate orderDate, String note, User createdBy) {
        this.orderNumber = orderNumber;
        this.supplier = supplier;
        this.orderDate = orderDate;
        this.note = normalizeNote(note);
        this.createdBy = createdBy;
        this.status = PurchaseOrderStatus.DRAFT;
        this.subtotal = ZERO;
        this.discountAmount = ZERO;
        this.taxAmount = ZERO;
        this.totalAmount = ZERO;
    }

    public void replaceDraft(Supplier supplier, LocalDate orderDate, String note, List<PurchaseOrderItemDraft> drafts) {
        this.supplier = supplier;
        this.orderDate = orderDate;
        this.note = normalizeNote(note);
        Map<Long, PurchaseOrderItem> existingByProduct = items.stream()
                .collect(Collectors.toMap(item -> item.getProduct().getId(), Function.identity()));
        items.removeIf(item -> drafts.stream()
                .noneMatch(draft -> draft.product().getId().equals(item.getProduct().getId())));
        drafts.forEach(draft -> {
            PurchaseOrderItem existing = existingByProduct.get(draft.product().getId());
            if (existing == null) {
                items.add(new PurchaseOrderItem(
                        this, draft.product(), draft.quantity(), draft.unitPrice(), draft.taxRate()));
            } else {
                existing.updateDraft(draft.product(), draft.quantity(), draft.unitPrice(), draft.taxRate());
            }
        });
        recalculateTotals();
    }

    public void markOrdered() {
        this.status = PurchaseOrderStatus.ORDERED;
        this.orderedAt = Instant.now();
    }

    public void receive() {
        this.status = PurchaseOrderStatus.RECEIVED;
        this.receivedAt = Instant.now();
    }

    public void cancel() {
        this.status = PurchaseOrderStatus.CANCELLED;
    }

    private void recalculateTotals() {
        subtotal = items.stream().map(PurchaseOrderItem::getLineSubtotal).reduce(ZERO, BigDecimal::add);
        discountAmount = ZERO;
        taxAmount = items.stream().map(PurchaseOrderItem::getLineTax).reduce(ZERO, BigDecimal::add);
        totalAmount = subtotal.subtract(discountAmount).add(taxAmount);
    }

    private String normalizeNote(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public Long getId() { return id; }
    public String getOrderNumber() { return orderNumber; }
    public Supplier getSupplier() { return supplier; }
    public LocalDate getOrderDate() { return orderDate; }
    public PurchaseOrderStatus getStatus() { return status; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getNote() { return note; }
    public Instant getOrderedAt() { return orderedAt; }
    public Instant getReceivedAt() { return receivedAt; }
    public User getCreatedBy() { return createdBy; }
    public List<PurchaseOrderItem> getItems() { return List.copyOf(items); }
}
