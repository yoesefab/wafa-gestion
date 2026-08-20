package com.wafabureau.gestion.mapper;

import com.wafabureau.gestion.dto.product.ProductResponse;
import com.wafabureau.gestion.model.Product;

public final class ProductMapper {
    private ProductMapper() { }

    public static ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(), product.getSku(), product.getName(), CategoryMapper.toReference(product.getCategory()),
                product.getUnitOfMeasure(), product.getPurchasePrice(), product.getSellingPrice(),
                product.getCurrentStock(), product.getMinimumStock(), product.isLowStock(), product.isActive(),
                product.getDeactivatedAt(), product.getCreatedAt(), product.getUpdatedAt(), product.getVersion()
        );
    }
}
