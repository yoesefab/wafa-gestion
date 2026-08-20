package com.wafabureau.gestion.repository.specification;
import com.wafabureau.gestion.model.*;
import com.wafabureau.gestion.enums.*;

import java.time.Instant;

import org.springframework.data.jpa.domain.Specification;

public final class StockMovementSpecifications {

    private StockMovementSpecifications() {
    }

    public static Specification<StockMovement> matches(
            Long productId,
            StockMovementType type,
            Instant occurredFrom,
            Instant occurredBefore
    ) {
        return (root, query, builder) -> {
            if (!Long.class.equals(query.getResultType())) {
                root.fetch("product");
                root.fetch("createdBy");
            }
            var predicate = builder.conjunction();
            if (productId != null) {
                predicate = builder.and(predicate, builder.equal(root.get("product").get("id"), productId));
            }
            if (type != null) {
                predicate = builder.and(predicate, builder.equal(root.get("movementType"), type));
            }
            if (occurredFrom != null) {
                predicate = builder.and(
                        predicate,
                        builder.greaterThanOrEqualTo(root.get("occurredAt"), occurredFrom)
                );
            }
            if (occurredBefore != null) {
                predicate = builder.and(predicate, builder.lessThan(root.get("occurredAt"), occurredBefore));
            }
            return predicate;
        };
    }
}
