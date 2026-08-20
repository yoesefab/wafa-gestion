package com.wafabureau.gestion.repository.specification;
import com.wafabureau.gestion.model.*;
import com.wafabureau.gestion.enums.*;

import java.time.LocalDate;

import org.springframework.data.jpa.domain.Specification;

public final class PurchaseOrderSpecifications {
    private PurchaseOrderSpecifications() { }

    public static Specification<PurchaseOrder> matches(
            String search,
            PurchaseOrderStatus status,
            Long supplierId,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        return (root, query, builder) -> {
            if (!Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                root.fetch("supplier", jakarta.persistence.criteria.JoinType.LEFT);
            }
            var predicate = builder.conjunction();
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicate = builder.and(predicate, builder.or(
                        builder.like(builder.lower(root.get("orderNumber")), pattern),
                        builder.like(builder.lower(root.get("supplier").get("name")), pattern)
                ));
            }
            if (status != null) predicate = builder.and(predicate, builder.equal(root.get("status"), status));
            if (supplierId != null) predicate = builder.and(predicate, builder.equal(root.get("supplier").get("id"), supplierId));
            if (dateFrom != null) predicate = builder.and(predicate, builder.greaterThanOrEqualTo(root.get("orderDate"), dateFrom));
            if (dateTo != null) predicate = builder.and(predicate, builder.lessThanOrEqualTo(root.get("orderDate"), dateTo));
            return predicate;
        };
    }
}
