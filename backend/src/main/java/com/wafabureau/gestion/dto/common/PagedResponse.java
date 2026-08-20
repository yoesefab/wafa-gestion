package com.wafabureau.gestion.dto.common;

import java.util.List;


public record PagedResponse<T>(List<T> data, PageMetadata page) {
}
