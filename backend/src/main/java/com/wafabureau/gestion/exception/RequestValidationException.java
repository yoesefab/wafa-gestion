package com.wafabureau.gestion.exception;
import com.wafabureau.gestion.dto.common.*;
import com.wafabureau.gestion.util.*;

public class RequestValidationException extends RuntimeException {

    public RequestValidationException(String message) {
        super(message);
    }
}
