package com.wafabureau.gestion.repository.specification;
import com.wafabureau.gestion.model.*;
import com.wafabureau.gestion.enums.*;

import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

public final class PartnerSpecifications {

    private PartnerSpecifications() {
    }

    public static <T extends PartnerEntity> Specification<T> matches(String search, Boolean active) {
        return (root, query, builder) -> {
            var predicate = builder.conjunction();
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicate = builder.and(predicate, builder.or(
                        builder.like(builder.lower(root.get("name")), pattern),
                        builder.like(builder.lower(root.get("ice")), pattern),
                        builder.like(builder.lower(root.get("email")), pattern),
                        builder.like(builder.lower(root.get("phone")), pattern)
                ));
            }
            if (active != null) {
                predicate = builder.and(predicate, builder.equal(root.get("active"), active));
            }
            return predicate;
        };
    }
}
