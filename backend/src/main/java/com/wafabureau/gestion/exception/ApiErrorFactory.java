package com.wafabureau.gestion.exception;
import com.wafabureau.gestion.dto.common.*;
import com.wafabureau.gestion.util.*;

import java.time.Instant;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;

import com.wafabureau.gestion.util.TraceIds;

public final class ApiErrorFactory {

    private ApiErrorFactory() {
    }

    public static ApiError create(
            HttpStatus status,
            String code,
            String detail,
            HttpServletRequest request
    ) {
        return create(status, code, detail, request, List.of());
    }

    public static ApiError create(
            HttpStatus status,
            String code,
            String detail,
            HttpServletRequest request,
            List<ApiFieldError> fieldErrors
    ) {
        return new ApiError(
                "about:blank",
                status.getReasonPhrase(),
                status.value(),
                code,
                detail,
                request.getRequestURI(),
                Instant.now(),
                TraceIds.currentOrCreate(),
                fieldErrors
        );
    }
}
