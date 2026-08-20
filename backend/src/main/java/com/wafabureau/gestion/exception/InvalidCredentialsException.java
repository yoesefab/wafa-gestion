package com.wafabureau.gestion.exception;
import com.wafabureau.gestion.dto.common.*;
import com.wafabureau.gestion.util.*;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("The email or password is incorrect.");
    }
}
