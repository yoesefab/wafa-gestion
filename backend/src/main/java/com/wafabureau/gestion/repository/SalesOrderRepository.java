package com.wafabureau.gestion.repository;
import com.wafabureau.gestion.model.*;

import java.util.Optional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import com.wafabureau.gestion.enums.SalesOrderStatus;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long>, JpaSpecificationExecutor<SalesOrder> {

    @EntityGraph(attributePaths = {"customer", "createdBy", "items", "items.product"})
    @Query("select distinct salesOrder from SalesOrder salesOrder where salesOrder.id = :id")
    Optional<SalesOrder> findDetailedById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select salesOrder from SalesOrder salesOrder where salesOrder.id = :id")
    Optional<SalesOrder> findByIdForUpdate(Long id);

    @Query(value = "SELECT nextval('sales_order_number_seq')", nativeQuery = true)
    long nextOrderNumberSequence();

    @Query("""
            select count(salesOrder) as orderCount,
                   coalesce(sum(salesOrder.totalAmount), 0) as revenue
            from SalesOrder salesOrder
            where salesOrder.status in :statuses
              and salesOrder.confirmedAt >= :from
              and salesOrder.confirmedAt < :to
            """)
    SalesAggregate aggregateSales(Collection<SalesOrderStatus> statuses, Instant from, Instant to);

    @Query(value = """
            select extract(month from (confirmed_at at time zone 'Africa/Casablanca')) as month_number,
                   count(*) as order_count,
                   coalesce(sum(total_amount), 0) as revenue
            from sales_orders
            where status in ('CONFIRMED', 'DELIVERED')
              and confirmed_at >= :from
              and confirmed_at < :to
            group by extract(month from (confirmed_at at time zone 'Africa/Casablanca'))
            order by extract(month from (confirmed_at at time zone 'Africa/Casablanca'))
            """, nativeQuery = true)
    List<MonthlySalesAggregate> aggregateMonthlySales(Instant from, Instant to);

    @Query("""
            select item.product.id as productId,
                   item.product.sku as sku,
                   item.product.name as name,
                   sum(item.quantity) as quantitySold,
                   coalesce(sum(item.lineTotal), 0) as revenue
            from SalesOrderItem item
            where item.salesOrder.status in :statuses
              and item.salesOrder.confirmedAt >= :from
              and item.salesOrder.confirmedAt < :to
            group by item.product.id, item.product.sku, item.product.name
            order by sum(item.quantity) desc, item.product.id asc
            """)
    List<TopProductAggregate> findTopProducts(
            Collection<SalesOrderStatus> statuses, Instant from, Instant to, Pageable pageable);

    interface SalesAggregate {
        Long getOrderCount();
        BigDecimal getRevenue();
    }

    interface MonthlySalesAggregate {
        Integer getMonthNumber();
        Long getOrderCount();
        BigDecimal getRevenue();
    }

    interface TopProductAggregate {
        Long getProductId();
        String getSku();
        String getName();
        Long getQuantitySold();
        BigDecimal getRevenue();
    }
}
