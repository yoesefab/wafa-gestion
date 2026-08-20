package com.wafabureau.gestion.exception;
import com.wafabureau.gestion.dto.common.*;
import com.wafabureau.gestion.util.*;

import java.util.Objects;

public class BusinessException extends RuntimeException {

    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code must not be null");
    }

    public String getCode() {
        return code;
    }
}
