package com.wafabureau.gestion.dto.common;

public record PageMetadata(
        int number,
        int size,
        long totalElements,
        int totalPages
) {
}
