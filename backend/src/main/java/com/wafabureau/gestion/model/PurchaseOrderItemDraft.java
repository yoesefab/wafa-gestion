package com.wafabureau.gestion.model;
import com.wafabureau.gestion.model.*;
import com.wafabureau.gestion.enums.*;
import com.wafabureau.gestion.dto.category.CategoryReference;

import java.math.BigDecimal;

import com.wafabureau.gestion.model.Product;

public record PurchaseOrderItemDraft(Product product, long quantity, BigDecimal unitPrice, BigDecimal taxRate) {
}
