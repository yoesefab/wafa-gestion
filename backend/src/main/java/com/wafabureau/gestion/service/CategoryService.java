package com.wafabureau.gestion.service;
import com.wafabureau.gestion.mapper.*;
import com.wafabureau.gestion.exception.*;
import com.wafabureau.gestion.model.*;
import com.wafabureau.gestion.repository.*;
import com.wafabureau.gestion.repository.specification.*;
import com.wafabureau.gestion.dto.auth.*;
import com.wafabureau.gestion.dto.category.*;
import com.wafabureau.gestion.dto.product.*;
import com.wafabureau.gestion.dto.customer.*;
import com.wafabureau.gestion.dto.supplier.*;
import com.wafabureau.gestion.dto.partner.*;
import com.wafabureau.gestion.dto.inventory.*;
import com.wafabureau.gestion.dto.sales.*;
import com.wafabureau.gestion.dto.purchase.*;
import com.wafabureau.gestion.enums.*;
import com.wafabureau.gestion.security.*;

import java.util.Locale;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wafabureau.gestion.dto.category.CategoryResponse;
import com.wafabureau.gestion.dto.category.CategoryUpdateRequest;
import com.wafabureau.gestion.dto.category.CategoryWriteRequest;
import com.wafabureau.gestion.dto.common.PagedResponse;
import com.wafabureau.gestion.dto.common.ArchiveRequest;
import com.wafabureau.gestion.exception.BusinessException;
import com.wafabureau.gestion.exception.RequestValidationException;
import com.wafabureau.gestion.exception.ResourceNotFoundException;
import com.wafabureau.gestion.util.PageRequestFactory;

@Service
public class CategoryService {

    private static final Set<String> SORT_FIELDS = Set.of("name", "createdAt");

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public CategoryResponse create(CategoryWriteRequest request) {
        String normalizedName = normalizeName(request.name());
        ensureNameAvailable(normalizedName, null);
        try {
            return CategoryMapper.toResponse(categoryRepository.saveAndFlush(
                    new Category(request.name(), request.description())
            ));
        } catch (DataIntegrityViolationException exception) {
            throw duplicateName();
        }
    }

    @Transactional(readOnly = true)
    public PagedResponse<CategoryResponse> list(
            int page,
            int size,
            String sort,
            String search,
            Boolean active
    ) {
        validateSearch(search);
        Page<Category> categories = categoryRepository.findAll(
                CategorySpecifications.matches(search, active),
                PageRequestFactory.create(page, size, sort, SORT_FIELDS)
        );
        return PageMapper.toResponse(categories, CategoryMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(Long id) {
        return CategoryMapper.toResponse(find(id));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryUpdateRequest request) {
        Category category = find(id);
        verifyVersion(category.getVersion(), request.version());
        ensureNameAvailable(normalizeName(request.name()), id);
        category.update(request.name(), request.description());
        try {
            return CategoryMapper.toResponse(categoryRepository.saveAndFlush(category));
        } catch (DataIntegrityViolationException exception) {
            throw duplicateName();
        }
    }

    @Transactional
    public CategoryResponse archive(Long id, ArchiveRequest request) {
        Category category = find(id);
        verifyVersion(category.getVersion(), request.version());
        if (!category.isActive()) {
            throw new BusinessException("INVALID_STATE_TRANSITION", "The category is already archived.");
        }
        category.archive();
        return CategoryMapper.toResponse(categoryRepository.saveAndFlush(category));
    }

    Category findActive(Long id) {
        Category category = find(id);
        if (!category.isActive()) {
            throw new BusinessException("INACTIVE_REFERENCE", "The selected category is inactive.");
        }
        return category;
    }

    private Category find(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    private void ensureNameAvailable(String normalizedName, Long excludedId) {
        boolean exists = excludedId == null
                ? categoryRepository.existsByNormalizedName(normalizedName)
                : categoryRepository.existsByNormalizedNameAndIdNot(normalizedName, excludedId);
        if (exists) {
            throw duplicateName();
        }
    }

    private String normalizeName(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private BusinessException duplicateName() {
        return new BusinessException("DUPLICATE_RESOURCE", "A category with this name already exists.");
    }

    private void validateSearch(String search) {
        if (search != null && search.trim().length() > 120) {
            throw new RequestValidationException("Category search must not exceed 120 characters.");
        }
    }

    private void verifyVersion(Long actual, Long expected) {
        if (!actual.equals(expected)) {
            throw new BusinessException("VERSION_CONFLICT", "The category was modified by another request.");
        }
    }
}
