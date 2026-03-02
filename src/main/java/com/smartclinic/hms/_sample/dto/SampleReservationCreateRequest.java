package com.smartclinic.hms._sample.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * ════════════════════════════════════════════════════════════════════════════
 * [샘플] SampleReservationCreateRequest — 요청 DTO (Java Record)
 * ════════════════════════════════════════════════════════════════════════════
 *
 * ■ 연관 API (API 명세서 v2.0 기준)
 * POST /reservation/create — 비회원 예약 생성
 * POST /staff/reservation/create — 전화 예약 등록 (ROLE_STAFF)
 *
 * ■ Java Record DTO 작성 핵심 규칙 (rule_spring.md §6)
 * 1. record 키워드 사용 — 불변성(immutability) 자동 보장
 * 2. Bean Validation 어노테이션을 컴포넌트(필드)에 직접 선언
 * 3. Controller에서 @Valid @RequestBody 로 JSON 바디 바인딩·검증
 * 4. @ModelAttribute 폼 바인딩 시에도 Record 사용 가능 (Spring 6.1+)
 * 5. 응답 DTO는 from(Entity) 정적 팩토리 메서드 패턴 사용
 *
 * ■ @Valid vs @Validated 구분 (rule_spring.md §7)
 * - @Valid : @RequestBody (JSON 바디)
 * - @Validated : Controller 클래스 레벨 + @RequestParam·@PathVariable
 * ════════════════════════════════════════════════════════════════════════════
 */
public record SampleReservationCreateRequest(

        // ── 환자 정보 ────────────────────────────────────────────────────────

        @NotBlank(message = "환자 이름은 필수입니다.") @Size(max = 20, message = "환자 이름은 20자 이내여야 합니다.") String patientName,

        @NotBlank(message = "연락처는 필수입니다.") @Pattern(regexp = "^\\d{3}-\\d{3,4}-\\d{4}$", message = "올바른 전화번호 형식이 아닙니다. (예: 010-1234-5678)") String patientPhone,

        @Email(message = "올바른 이메일 형식이 아닙니다.") // null 허용 — @Email은 null pass
        String patientEmail, // 선택 필드

        // ── 예약 대상 ────────────────────────────────────────────────────────

        @NotNull(message = "진료과 ID는 필수입니다.") Long departmentId,

        @NotNull(message = "의사 ID는 필수입니다.") Long doctorId,

        // ── 예약 일시 ────────────────────────────────────────────────────────

        @NotNull(message = "예약 날짜는 필수입니다.") @FutureOrPresent(message = "예약 날짜는 오늘 이후여야 합니다.") LocalDate reservationDate,

        @NotBlank(message = "예약 시간은 필수입니다.") @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]0$", message = "예약 시간은 HH:mm 형식이며 30분 단위여야 합니다. (예: 09:00, 14:30)") String timeSlot,

        // ── LLM 경유 예약 (선택) ─────────────────────────────────────────────
        // null 허용 — LLM 증상 추천 후 예약 시 전달 → is_used = TRUE 업데이트
        Long llmRecommendationId

) {
    // Record는 불변 → 별도의 setter 없음
    // 추가 검증 로직이 필요하면 compact constructor 또는 Service에서 처리

    /*
     * ── 컴팩트 생성자 (Compact Constructor) 예시 ──────────────────────────
     * 인스턴스 생성 시 추가 검증이 필요한 경우 사용
     *
     * public SampleReservationCreateRequest {
     * // null-safe trim
     * if (patientName != null) patientName = patientName.trim();
     * if (patientPhone != null) patientPhone = patientPhone.trim();
     * }
     */
}
