package com.wafabureau.gestion.model;
import com.wafabureau.gestion.enums.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;

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

import com.wafabureau.gestion.model.AuditableEntity;

@Entity
@Table(name = "products")
public class Product extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String sku;

    @Column(name = "normalized_sku", nullable = false, length = 80, unique = true)
    private String normalizedSku;

    @Column(nullable = false, length = 180)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_of_measure", nullable = false, length = 30)
    private UnitOfMeasure unitOfMeasure;

    @Column(name = "purchase_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "selling_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal sellingPrice;

    @Column(name = "current_stock", nullable = false)
    private Long currentStock;

    @Column(name = "minimum_stock", nullable = false)
    private Long minimumStock;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    protected Product() {
    }

    public Product(
            String sku,
            String name,
            Category category,
            UnitOfMeasure unitOfMeasure,
            BigDecimal purchasePrice,
            BigDecimal sellingPrice,
            Long minimumStock
    ) {
        applyDetails(sku, name, category, unitOfMeasure, purchasePrice, sellingPrice, minimumStock);
        this.currentStock = 0L;
        this.active = true;
    }

    public void update(
            String sku,
            String name,
            Category category,
            UnitOfMeasure unitOfMeasure,
            BigDecimal purchasePrice,
            BigDecimal sellingPrice,
            Long minimumStock
    ) {
        applyDetails(sku, name, category, unitOfMeasure, purchasePrice, sellingPrice, minimumStock);
    }

    public void archive() {
        this.active = false;
        this.deactivatedAt = Instant.now();
    }

    public boolean isLowStock() {
        return currentStock <= minimumStock;
    }

    public void replaceCurrentStock(Long newStock) {
        this.currentStock = newStock;
    }

    private void applyDetails(
            String sku,
            String name,
            Category category,
            UnitOfMeasure unitOfMeasure,
            BigDecimal purchasePrice,
            BigDecimal sellingPrice,
            Long minimumStock
    ) {
        this.sku = sku.trim();
        this.normalizedSku = this.sku.toUpperCase(Locale.ROOT);
        this.name = name.trim();
        this.category = category;
        this.unitOfMeasure = unitOfMeasure;
        this.purchasePrice = purchasePrice;
        this.sellingPrice = sellingPrice;
        this.minimumStock = minimumStock;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public Category getCategory() {
        return category;
    }

    public UnitOfMeasure getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public Long getCurrentStock() {
        return currentStock;
    }

    public Long getMinimumStock() {
        return minimumStock;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getDeactivatedAt() {
        return deactivatedAt;
    }
}
