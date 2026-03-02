package com.smartclinic.hms._sample;

import org.springframework.http.HttpStatus;

/**
 * ════════════════════════════════════════════════════════════════════════════
 * [샘플] SampleBusinessException — 비즈니스 예외 작성 가이드
 * ════════════════════════════════════════════════════════════════════════════
 *
 * ■ 실제 구현 위치
 * com.smartclinic.hms.common.exception.BusinessException
 *
 * ■ 이 클래스는 _sample 패키지의 독립 실행을 위한 임시 구현입니다.
 * 실제 개발 시에는 common.exception.BusinessException을 사용하세요.
 *
 * ■ 비즈니스 예외 핵심 규칙 (rule_spring.md §2)
 * 1. GlobalExceptionHandler가 잡아서 JSON { success, errorCode, message } 반환
 * 2. errorCode — API 명세서의 에러 코드 목록 사용 (§15)
 * 3. httpStatus — 도메인 의미에 맞는 HTTP 상태코드 선택 (§7.1.9)
 * 4. 편의 팩토리 메서드로 자주 쓰는 케이스 간단히 표현
 *
 * ■ API 명세서 에러 코드 목록 (§15)
 * - DUPLICATE_RESERVATION (409) : 동일 의사·날짜·시간 중복 예약
 * - INVALID_TIME_SLOT (400) : 유효하지 않은 시간 슬롯
 * - DOCTOR_NOT_AVAILABLE (400) : 해당 날짜 의사 진료 불가
 * - RESERVATION_NOT_FOUND (404) : 예약 ID 없음
 * - CANNOT_CANCEL_COMPLETED (409) : 진료 완료 예약 취소 불가
 * - INVALID_STATUS_TRANSITION(409) : 허용되지 않는 상태 전이
 * - ALREADY_CANCELLED (409) : 이미 취소된 예약
 * - ALREADY_COMPLETED (409) : 이미 완료된 예약
 * - UNAUTHORIZED (401) : 미인증 접근
 * - ACCESS_DENIED (403) : 권한 없는 접근
 * - NOT_OWN_PATIENT (403) : 본인 담당 아닌 환자 접근
 * - LLM_SERVICE_UNAVAILABLE (503) : Claude API 호출 실패
 * - LLM_PARSE_ERROR (500) : LLM 응답 파싱 오류
 * - RESOURCE_NOT_FOUND (404) : 리소스 없음
 * - DUPLICATE_USERNAME (409) : 이미 존재하는 로그인 ID
 * - VALIDATION_ERROR (400) : 필수 필드 누락 또는 형식 오류
 * ════════════════════════════════════════════════════════════════════════════
 */
public class SampleBusinessException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    /**
     * 기본 생성자 — errorCode, message, httpStatus 명시적 지정
     *
     * 사용 예:
     * throw new SampleBusinessException("DUPLICATE_RESERVATION",
     * "해당 시간대는 이미 예약이 완료되었습니다.", HttpStatus.CONFLICT);
     */
    public SampleBusinessException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    // ════════════════════════════════════════════════════════════════════════
    // 편의 팩토리 메서드 — 자주 사용하는 케이스 간단히 표현
    // ════════════════════════════════════════════════════════════════════════

    /** 404 Not Found — 리소스를 찾을 수 없는 경우 */
    public static SampleBusinessException notFound(String message) {
        return new SampleBusinessException("RESOURCE_NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }

    /** 409 Conflict — 중복 생성 시도 */
    public static SampleBusinessException conflict(String errorCode, String message) {
        return new SampleBusinessException(errorCode, message, HttpStatus.CONFLICT);
    }

    /** 403 Forbidden — 권한 없는 접근 */
    public static SampleBusinessException forbidden(String message) {
        return new SampleBusinessException("ACCESS_DENIED", message, HttpStatus.FORBIDDEN);
    }

    /** 400 Bad Request — 잘못된 요청 (비즈니스 규칙 위반) */
    public static SampleBusinessException badRequest(String errorCode, String message) {
        return new SampleBusinessException(errorCode, message, HttpStatus.BAD_REQUEST);
    }

    /** 409 Conflict — 상태 전이 오류 */
    public static SampleBusinessException invalidStatusTransition(String message) {
        return new SampleBusinessException("INVALID_STATUS_TRANSITION", message, HttpStatus.CONFLICT);
    }

    /*
     * ── GlobalExceptionHandler 연동 예시 ──────────────────────────────────
     * (실제 구현: common.exception.GlobalExceptionHandler)
     *
     * @ExceptionHandler(BusinessException.class)
     * public ResponseEntity<ErrorResponse> handleBusiness(
     * BusinessException ex, HttpServletRequest req) {
     *
     * // SSR 페이지 요청 vs AJAX 요청 구분
     * String accept = req.getHeader("Accept");
     * if (accept != null && accept.contains("text/html")) {
     * // 폼 제출 — 에러를 JSON으로 반환 (클라이언트에서 처리)
     * }
     *
     * return ResponseEntity
     * .status(ex.getHttpStatus())
     * .body(ErrorResponse.of(ex.getErrorCode(), ex.getMessage(), req));
     * }
     */
}
