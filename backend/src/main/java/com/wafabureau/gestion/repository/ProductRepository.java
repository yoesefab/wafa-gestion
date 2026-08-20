package com.wafabureau.gestion.repository;
import com.wafabureau.gestion.model.*;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    boolean existsByNormalizedSku(String normalizedSku);

    boolean existsByNormalizedSkuAndIdNot(String normalizedSku, Long id);

    @EntityGraph(attributePaths = "category")
    @Query("select product from Product product where product.id = :id")
    Optional<Product> findDetailedById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select product from Product product where product.id = :id")
    Optional<Product> findByIdForStockUpdate(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select product from Product product where product.id in :ids order by product.id")
    List<Product> findAllByIdForStockUpdate(Iterable<Long> ids);

    long countByActiveTrue();

    @Query("""
            select count(product)
            from Product product
            where product.active = true
              and product.currentStock <= product.minimumStock
            """)
    long countActiveLowStock();

    @Query("""
            select product.id as id,
                   product.sku as sku,
                   product.name as name,
                   product.currentStock as currentStock,
                   product.minimumStock as minimumStock
            from Product product
            where product.active = true
              and product.currentStock <= product.minimumStock
            order by (product.currentStock - product.minimumStock) asc, product.name asc, product.id asc
            """)
    List<LowStockProductProjection> findActiveLowStock(Pageable pageable);

    interface LowStockProductProjection {
        Long getId();
        String getSku();
        String getName();
        Long getCurrentStock();
        Long getMinimumStock();
    }
}
