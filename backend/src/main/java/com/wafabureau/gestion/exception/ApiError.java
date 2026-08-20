package com.wafabureau.gestion.exception;
import com.wafabureau.gestion.dto.common.*;
import com.wafabureau.gestion.util.*;

import java.time.Instant;
import java.util.List;

public record ApiError(
        String type,
        String title,
        int status,
        String code,
        String detail,
        String instance,
        Instant timestamp,
        String traceId,
        List<ApiFieldError> fieldErrors
) {
    public ApiError {
        fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }
}
