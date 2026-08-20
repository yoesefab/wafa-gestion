package com.wafabureau.gestion.mapper;

import java.util.function.Function;

import org.springframework.data.domain.Page;

import com.wafabureau.gestion.dto.common.PageMetadata;
import com.wafabureau.gestion.dto.common.PagedResponse;

public final class PageMapper {
    private PageMapper() { }

    public static <S, T> PagedResponse<T> toResponse(Page<S> source, Function<S, T> mapper) {
        return new PagedResponse<>(
                source.getContent().stream().map(mapper).toList(),
                new PageMetadata(source.getNumber(), source.getSize(), source.getTotalElements(), source.getTotalPages())
        );
    }
}
