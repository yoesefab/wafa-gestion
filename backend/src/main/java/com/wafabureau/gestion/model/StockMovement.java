package com.wafabureau.gestion.model;
import com.wafabureau.gestion.enums.*;

import java.time.Instant;

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
import jakarta.persistence.Table;

import com.wafabureau.gestion.model.Product;
import com.wafabureau.gestion.model.User;

@Entity
@Table(name = "stock_movements")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30, updatable = false)
    private StockMovementType movementType;

    @Column(name = "quantity_delta", nullable = false, updatable = false)
    private Long quantityDelta;

    @Column(name = "stock_before", nullable = false, updatable = false)
    private Long stockBefore;

    @Column(name = "stock_after", nullable = false, updatable = false)
    private Long stockAfter;

    @Column(nullable = false, length = 120, updatable = false)
    private String reference;

    @Column(nullable = false, length = 180, updatable = false)
    private String reason;

    @Column(length = 1000, updatable = false)
    private String note;

    @Column(name = "sales_order_item_id", updatable = false)
    private Long salesOrderItemId;

    @Column(name = "purchase_order_item_id", updatable = false)
    private Long purchaseOrderItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false, updatable = false)
    private User createdBy;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected StockMovement() {
    }

    public StockMovement(
            Product product,
            StockMovementType movementType,
            long quantityDelta,
            long stockBefore,
            long stockAfter,
            String reference,
            String reason,
            String note,
            Long salesOrderItemId,
            Long purchaseOrderItemId,
            User createdBy
    ) {
        this.product = product;
        this.movementType = movementType;
        this.quantityDelta = quantityDelta;
        this.stockBefore = stockBefore;
        this.stockAfter = stockAfter;
        this.reference = reference.trim();
        this.reason = reason.trim();
        this.note = note == null || note.isBlank() ? null : note.trim();
        this.salesOrderItemId = salesOrderItemId;
        this.purchaseOrderItemId = purchaseOrderItemId;
        this.createdBy = createdBy;
        this.occurredAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public StockMovementType getStockMovementType() {
        return movementType;
    }

    public Long getQuantityDelta() {
        return quantityDelta;
    }

    public Long getStockBefore() {
        return stockBefore;
    }

    public Long getStockAfter() {
        return stockAfter;
    }

    public String getReference() {
        return reference;
    }

    public String getReason() {
        return reason;
    }

    public String getNote() {
        return note;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public Long getSalesOrderItemId() {
        return salesOrderItemId;
    }

    public Long getPurchaseOrderItemId() {
        return purchaseOrderItemId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
