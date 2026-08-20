package com.wafabureau.gestion.util;

import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.wafabureau.gestion.exception.RequestValidationException;

public final class PageRequestFactory {

    private PageRequestFactory() {
    }

    public static Pageable create(int page, int size, String sortValue, Set<String> allowedFields) {
        if (page < 0) {
            throw new RequestValidationException("Page number must be zero or greater.");
        }
        if (size < 1 || size > 100) {
            throw new RequestValidationException("Page size must be between 1 and 100.");
        }

        String[] parts = sortValue.split(",", -1);
        if (parts.length != 2 || !allowedFields.contains(parts[0])) {
            throw new RequestValidationException("The requested sort is not supported.");
        }

        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(parts[1]);
        } catch (IllegalArgumentException exception) {
            throw new RequestValidationException("Sort direction must be 'asc' or 'desc'.");
        }

        Sort sort = Sort.by(direction, parts[0]).and(Sort.by(Sort.Direction.ASC, "id"));
        return PageRequest.of(page, size, sort);
    }
}
