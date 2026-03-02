package com.smartclinic.hms.common.exception;

import org.springframework.http.HttpStatus;

/**
 * ════════════════════════════════════════════════════════════════════════════
 * 도메인 비즈니스 예외 — GlobalExceptionHandler에서 JSON 응답으로 변환
 * ════════════════════════════════════════════════════════════════════════════
 *
 * ■ 사용 규칙 (rule_spring.md §2)
 *   1. GlobalExceptionHandler가 잡아서 JSON { success, errorCode, message } 반환
 *   2. errorCode — API 명세서 §15 에러 코드 목록 사용
 *   3. httpStatus — 도메인 의미에 맞는 HTTP 상태코드 선택 (rule_spring.md §9)
 *   4. 편의 팩토리 메서드로 자주 쓰는 케이스를 간단히 표현
 *
 * ■ API 명세서 §15 에러 코드 목록
 *   DUPLICATE_RESERVATION     (409) : 동일 의사·날짜·시간 중복 예약
 *   INVALID_TIME_SLOT         (400) : 유효하지 않은 시간 슬롯
 *   DOCTOR_NOT_AVAILABLE      (400) : 해당 날짜 의사 진료 불가
 *   RESERVATION_NOT_FOUND     (404) : 예약 ID 없음
 *   CANNOT_CANCEL_COMPLETED   (409) : 진료 완료 예약 취소 불가
 *   INVALID_STATUS_TRANSITION (409) : 허용되지 않는 상태 전이
 *   ALREADY_CANCELLED         (409) : 이미 취소된 예약
 *   ALREADY_COMPLETED         (409) : 이미 완료된 예약
 *   UNAUTHORIZED              (401) : 미인증 접근
 *   ACCESS_DENIED             (403) : 권한 없는 접근
 *   NOT_OWN_PATIENT           (403) : 본인 담당 아닌 환자 접근
 *   LLM_SERVICE_UNAVAILABLE   (503) : Claude API 호출 실패
 *   LLM_PARSE_ERROR           (500) : LLM 응답 파싱 오류
 *   RESOURCE_NOT_FOUND        (404) : 리소스 없음
 *   DUPLICATE_USERNAME        (409) : 이미 존재하는 로그인 ID
 *   VALIDATION_ERROR          (400) : 필수 필드 누락 또는 형식 오류
 *
 * ■ 사용 예
 *   throw new CustomException("DUPLICATE_RESERVATION", "해당 시간대는 이미 예약이 완료되었습니다.", HttpStatus.CONFLICT);
 *   throw CustomException.notFound("예약을 찾을 수 없습니다. ID: " + id);
 * ════════════════════════════════════════════════════════════════════════════
 */
public class CustomException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    /**
     * 기본 생성자 — errorCode, message, httpStatus 명시적 지정.
     * API 명세서 §15 에러 코드 목록을 errorCode에 사용한다.
     */
    public CustomException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode  = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode()    { return errorCode; }
    public HttpStatus getHttpStatus() { return httpStatus; }

    // ════════════════════════════════════════════════════════════════════════
    // 편의 팩토리 메서드 — 자주 사용하는 케이스 간단히 표현
    // ════════════════════════════════════════════════════════════════════════

    /** 404 Not Found — 리소스를 찾을 수 없는 경우 */
    public static CustomException notFound(String message) {
        return new CustomException("RESOURCE_NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }

    /** 409 Conflict — 중복 생성 또는 상태 충돌 */
    public static CustomException conflict(String errorCode, String message) {
        return new CustomException(errorCode, message, HttpStatus.CONFLICT);
    }

    /** 403 Forbidden — 권한 없는 접근 */
    public static CustomException forbidden(String message) {
        return new CustomException("ACCESS_DENIED", message, HttpStatus.FORBIDDEN);
    }

    /** 401 Unauthorized — 미인증 접근 */
    public static CustomException unauthorized(String message) {
        return new CustomException("UNAUTHORIZED", message, HttpStatus.UNAUTHORIZED);
    }

    /** 400 Bad Request — 잘못된 요청 (비즈니스 규칙 위반) */
    public static CustomException badRequest(String errorCode, String message) {
        return new CustomException(errorCode, message, HttpStatus.BAD_REQUEST);
    }

    /** 409 Conflict — 허용되지 않는 상태 전이 */
    public static CustomException invalidStatusTransition(String message) {
        return new CustomException("INVALID_STATUS_TRANSITION", message, HttpStatus.CONFLICT);
    }

    /** 503 Service Unavailable — 외부 서비스(LLM 등) 호출 실패 */
    public static CustomException serviceUnavailable(String message) {
        return new CustomException("LLM_SERVICE_UNAVAILABLE", message, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
