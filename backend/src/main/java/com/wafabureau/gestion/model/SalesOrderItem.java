package com.wafabureau.gestion.model;
import com.wafabureau.gestion.enums.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.wafabureau.gestion.model.Product;

@Entity
@Table(name = "sales_order_items")
public class SalesOrderItem {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_order_id", nullable = false)
    private SalesOrder salesOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Long quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Column(name = "line_subtotal", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineSubtotal;

    @Column(name = "line_tax", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineTax;

    @Column(name = "line_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineTotal;

    protected SalesOrderItem() {
    }

    SalesOrderItem(SalesOrder salesOrder, Product product, long quantity, BigDecimal taxRate) {
        this.salesOrder = salesOrder;
        apply(product, quantity, taxRate);
    }

    void updateDraft(Product product, long quantity, BigDecimal taxRate) {
        apply(product, quantity, taxRate);
    }

    private void apply(Product product, long quantity, BigDecimal taxRate) {
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = money(product.getSellingPrice());
        this.taxRate = taxRate.setScale(2, RoundingMode.HALF_UP);
        this.lineSubtotal = money(this.unitPrice.multiply(BigDecimal.valueOf(quantity)));
        this.lineTax = money(this.lineSubtotal.multiply(this.taxRate).divide(ONE_HUNDRED));
        this.lineTotal = money(this.lineSubtotal.add(this.lineTax));
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public Long getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getTaxRate() { return taxRate; }
    public BigDecimal getLineSubtotal() { return lineSubtotal; }
    public BigDecimal getLineTax() { return lineTax; }
    public BigDecimal getLineTotal() { return lineTotal; }
}
