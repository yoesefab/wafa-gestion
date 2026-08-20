package com.wafabureau.gestion.repository.specification;
import com.wafabureau.gestion.model.*;
import com.wafabureau.gestion.enums.*;

import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> matches(
            String search,
            Long categoryId,
            Boolean active,
        Boolean lowStock
    ) {
        return (root, query, builder) -> {
            if (!Long.class.equals(query.getResultType())) {
                root.fetch("category");
            }
            var predicate = builder.conjunction();
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicate = builder.and(predicate, builder.or(
                        builder.like(builder.lower(root.get("sku")), pattern),
                        builder.like(builder.lower(root.get("name")), pattern)
                ));
            }
            if (categoryId != null) {
                predicate = builder.and(predicate, builder.equal(root.get("category").get("id"), categoryId));
            }
            if (active != null) {
                predicate = builder.and(predicate, builder.equal(root.get("active"), active));
            }
            if (Boolean.TRUE.equals(lowStock)) {
                predicate = builder.and(
                        predicate,
                        builder.lessThanOrEqualTo(root.get("currentStock"), root.get("minimumStock"))
                );
            }
            return predicate;
        };
    }
}
