package com.wafabureau.gestion.mapper;

import com.wafabureau.gestion.dto.category.CategoryReference;
import com.wafabureau.gestion.dto.category.CategoryResponse;
import com.wafabureau.gestion.model.Category;

public final class CategoryMapper {
    private CategoryMapper() { }

    public static CategoryReference toReference(Category category) {
        return new CategoryReference(category.getId(), category.getName());
    }

    public static CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(), category.getName(), category.getDescription(), category.isActive(),
                category.getDeactivatedAt(), category.getCreatedAt(), category.getUpdatedAt(), category.getVersion()
        );
    }
}
