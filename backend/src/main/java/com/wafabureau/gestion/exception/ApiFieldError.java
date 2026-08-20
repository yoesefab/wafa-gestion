package com.wafabureau.gestion.exception;
import com.wafabureau.gestion.dto.common.*;
import com.wafabureau.gestion.util.*;

public record ApiFieldError(
        String field,
        String code,
        String message
) {
}
