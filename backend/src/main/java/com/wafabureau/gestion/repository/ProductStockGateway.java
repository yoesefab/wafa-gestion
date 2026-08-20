package com.wafabureau.gestion.repository;
import com.wafabureau.gestion.model.*;

import java.util.List;

import org.springframework.stereotype.Component;

import com.wafabureau.gestion.exception.ResourceNotFoundException;

@Component
public class ProductStockGateway {

    private final ProductRepository productRepository;

    public ProductStockGateway(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product lock(Long productId) {
        return productRepository.findByIdForStockUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
    }

    public List<Product> lockAll(List<Long> productIds) {
        return productRepository.findAllByIdForStockUpdate(productIds);
    }

    public void replaceStock(Product product, long newStock) {
        product.replaceCurrentStock(newStock);
    }
}
