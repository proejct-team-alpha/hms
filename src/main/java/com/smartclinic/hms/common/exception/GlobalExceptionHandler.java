package com.smartclinic.hms.common.exception;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
 *   5. Exception                       — 예측 불가 예외 (폴백)
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
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ════════════════════════════════════════════════════════════════════════
    // ErrorResponse — 공통 에러 응답 포맷
    // ════════════════════════════════════════════════════════════════════════

    /**
     * 공통 에러 응답.
     *
     * <ul>
     *   <li>{@code success}   — 항상 {@code false}</li>
     *   <li>{@code errorCode} — API 명세서 §15 에러 코드</li>
     *   <li>{@code message}   — 사람이 읽을 수 있는 메시지</li>
     *   <li>{@code timestamp} — 오류 발생 시각 (ISO-8601)</li>
     *   <li>{@code path}      — 요청 URI</li>
     *   <li>{@code traceId}   — Micrometer Tracing 연동 시 자동 채워짐</li>
     *   <li>{@code details}   — Validation 오류 필드 목록 등 부가 정보</li>
     * </ul>
     */
    public record ErrorResponse(
            boolean success,
            String errorCode,
            String message,
            Instant timestamp,
            String path,
            String traceId,
            Map<String, Object> details
    ) {
        /** 단순 오류 (details 없음) */
        public static ErrorResponse of(String errorCode, String message, HttpServletRequest req) {
            return new ErrorResponse(
                    false, errorCode, message,
                    Instant.now(),
                    req.getRequestURI(),
                    MDC.get("traceId"),
                    Map.of()
            );
        }

        /** Validation 오류 (fields 포함) */
        public static ErrorResponse ofValidation(
                String message, Map<String, Object> details, HttpServletRequest req) {
            return new ErrorResponse(
                    false, "VALIDATION_ERROR", message,
                    Instant.now(),
                    req.getRequestURI(),
                    MDC.get("traceId"),
                    details
            );
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 1. Bean Validation — @Valid (@RequestBody)
    // ════════════════════════════════════════════════════════════════════════

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest req) {

        Map<String, Object> details = ex.getBindingResult()
                .getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        e -> e.getDefaultMessage() != null ? e.getDefaultMessage() : "invalid",
                        (a, b) -> a   // 동일 필드 중복 시 첫 번째 메시지 유지
                ));

        String message = details.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", "));

        return ResponseEntity.badRequest()
                .body(ErrorResponse.ofValidation(message, details, req));
    }

    // ════════════════════════════════════════════════════════════════════════
    // 2. Bean Validation — @Validated (Path/Query 파라미터)
    // ════════════════════════════════════════════════════════════════════════

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest req) {

        Map<String, Object> details = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        v -> v.getMessage(),
                        (a, b) -> a
                ));

        return ResponseEntity.badRequest()
                .body(ErrorResponse.ofValidation(ex.getMessage(), details, req));
    }

    // ════════════════════════════════════════════════════════════════════════
    // 3. 도메인 비즈니스 예외
    // ════════════════════════════════════════════════════════════════════════

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            CustomException ex, HttpServletRequest req) {

        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(ErrorResponse.of(ex.getErrorCode(), ex.getMessage(), req));
    }

    // ════════════════════════════════════════════════════════════════════════
    // 4. JPA 리소스 없음
    // ════════════════════════════════════════════════════════════════════════

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            EntityNotFoundException ex, HttpServletRequest req) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("RESOURCE_NOT_FOUND", ex.getMessage(), req));
    }

    // ════════════════════════════════════════════════════════════════════════
    // 5. 예측 불가 예외 (폴백)
    // ════════════════════════════════════════════════════════════════════════

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex, HttpServletRequest req) {

        log.error("[GlobalExceptionHandler] 예측 불가 예외: path={}", req.getRequestURI(), ex);

        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of("INTERNAL_ERROR", "서버 오류가 발생했습니다.", req));
    }
}
