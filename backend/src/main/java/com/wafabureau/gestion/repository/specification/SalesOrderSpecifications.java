package com.wafabureau.gestion.repository.specification;
import com.wafabureau.gestion.model.*;
import com.wafabureau.gestion.enums.*;

import java.time.LocalDate;

import org.springframework.data.jpa.domain.Specification;

public final class SalesOrderSpecifications {
    private SalesOrderSpecifications() { }

    public static Specification<SalesOrder> matches(
            String search,
            SalesOrderStatus status,
            Long customerId,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        return (root, query, builder) -> {
            if (!Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                root.fetch("customer", jakarta.persistence.criteria.JoinType.LEFT);
            }
            var predicate = builder.conjunction();
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicate = builder.and(predicate, builder.or(
                        builder.like(builder.lower(root.get("orderNumber")), pattern),
                        builder.like(builder.lower(root.get("customer").get("name")), pattern)
                ));
            }
            if (status != null) predicate = builder.and(predicate, builder.equal(root.get("status"), status));
            if (customerId != null) predicate = builder.and(predicate, builder.equal(root.get("customer").get("id"), customerId));
            if (dateFrom != null) predicate = builder.and(predicate, builder.greaterThanOrEqualTo(root.get("orderDate"), dateFrom));
            if (dateTo != null) predicate = builder.and(predicate, builder.lessThanOrEqualTo(root.get("orderDate"), dateTo));
            return predicate;
        };
    }
}
