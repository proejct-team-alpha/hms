package com.smartclinic.hms.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

public final class SsrValidationViewSupport {

    public static final String INPUT_CHECK_MESSAGE = "입력값을 확인해 주세요.";

    private SsrValidationViewSupport() {
    }

    public static void applyErrors(HttpServletRequest req, BindingResult bindingResult) {
        req.setAttribute("errorMessage", INPUT_CHECK_MESSAGE);

        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            req.setAttribute(fieldError.getField() + "Error", fieldError.getDefaultMessage());
        }
    }
}
