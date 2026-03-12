package com.smartclinic.hms.common.exception;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * SSR(@Controller) 전용 예외 핸들러 — 에러 페이지(뷰)를 반환한다.
 * JSON API(@RestController) 예외는 GlobalExceptionHandler가 처리한다.
 */
@Slf4j
@ControllerAdvice(annotations = Controller.class)
public class SsrExceptionHandler {

    @ExceptionHandler(Exception.class)
    public String handleSsrException(Exception ex, HttpServletRequest req, Model model) {
        log.error("[SsrExceptionHandler] SSR 예외: path={}", req.getRequestURI(), ex);
        model.addAttribute("errorMessage", "오류가 발생했습니다.");
        model.addAttribute("path", req.getRequestURI());
        return "error/500";
    }
}
