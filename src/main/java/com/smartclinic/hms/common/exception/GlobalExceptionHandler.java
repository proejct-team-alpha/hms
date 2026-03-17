package com.smartclinic.hms.common.exception;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.smartclinic.hms.common.util.Resp;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/**
 * ════════════════════════════════════════════════════════════════════════════
 * 전역 예외 핸들러 — 모든 예외를 공통 ErrorResponse 포맷으로 반환
 * ════════════════════════════════════════════════════════════════════════════
 *
 * ■ 처리 대상 (rule_spring.md §2)
 *   1. MethodArgumentNotValidException — @Valid (@RequestBody 검증 실패)
 *   2. ConstraintViolationException    — @Validated (Path/Query 파라미터 검증 실패)
 *   3. CustomException                 — 도메인 비즈니스 예외
 *   4. EntityNotFoundException         — JPA 리소스 없음
 *   5. NoResourceFoundException       — 정적 리소스/라우트 없음
 *   6. Exception                       — 예측 불가 예외 (폴백)
 *
 * ■ 응답 포맷
 *   { "success": false, "errorCode": "...", "message": "...",
 *     "timestamp": "...", "path": "...", "traceId": "...", "details": {} }
 *
 * ■ SSR + AJAX 공용
 *   POST 성공 시 → Controller에서 302 리다이렉트
 *   POST/GET 실패 시 → 이 핸들러가 JSON 반환 (클라이언트 JS에서 처리)
 * ════════════════════════════════════════════════════════════════════════════
 */
@Slf4j
@RestControllerAdvice(annotations = org.springframework.web.bind.annotation.RestController.class)
public class GlobalExceptionHandler {

    // ════════════════════════════════════════════════════════════════════════
    // 1. Bean Validation — @Valid (@RequestBody)
    // ════════════════════════════════════════════════════════════════════════

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Resp<?>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest req) {

        String message = ex.getBindingResult()
                .getFieldErrors().stream()
                .map(e -> e.getField() + ": " + (e.getDefaultMessage() != null ? e.getDefaultMessage() : "invalid"))
                .collect(Collectors.joining(", "));

        return Resp.fail(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    // ════════════════════════════════════════════════════════════════════════
    // 2. Bean Validation — @Validated (Path/Query 파라미터)
    // ════════════════════════════════════════════════════════════════════════

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Resp<?>> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest req) {

        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));

        return Resp.fail(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    // ════════════════════════════════════════════════════════════════════════
    // 3. 도메인 비즈니스 예외
    // ════════════════════════════════════════════════════════════════════════

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Resp<?>> handleBusiness(
            CustomException ex, HttpServletRequest req) {

        return Resp.fail(ex.getHttpStatus(), ex.getErrorCode(), ex.getMessage());
    }

    // ════════════════════════════════════════════════════════════════════════
    // 4. JPA 리소스 없음
    // ════════════════════════════════════════════════════════════════════════

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Resp<?>> handleNotFound(
            EntityNotFoundException ex, HttpServletRequest req) {

        return Resp.fail(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage());
    }

    // ════════════════════════════════════════════════════════════════════════
    // 5. 정적 리소스 없음 (favicon.ico 등)
    // ════════════════════════════════════════════════════════════════════════

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Resp<?>> handleNoResourceFound(
            NoResourceFoundException ex, HttpServletRequest req) {

        log.debug("[GlobalExceptionHandler] 리소스 없음: path={}", req.getRequestURI());

        return Resp.fail(HttpStatus.NOT_FOUND, "ROUTE_NOT_FOUND", ex.getMessage());
    }

    // ════════════════════════════════════════════════════════════════════════
    // 6. 예측 불가 예외 (폴백)
    // ════════════════════════════════════════════════════════════════════════

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Resp<?>> handleUnexpected(
            Exception ex, HttpServletRequest req) {

        log.error("[GlobalExceptionHandler] 예측 불가 예외: path={}", req.getRequestURI(), ex);

        return Resp.fail(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 오류가 발생했습니다.");
    }
}
