package com.smartclinic.hms._sample.dto;

import com.smartclinic.hms._sample.SampleReservation;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ════════════════════════════════════════════════════════════════════════════
 * [샘플] SampleReservationResponse — 응답 DTO (Java Record)
 * ════════════════════════════════════════════════════════════════════════════
 *
 * ■ 응답 DTO 작성 핵심 규칙
 *   1. from(Entity) 정적 팩토리 메서드 패턴 — 엔티티 → DTO 변환
 *   2. 엔티티를 직접 Controller/View에 노출하지 않음 (계층 분리)
 *   3. 클라이언트에 필요한 필드만 포함 (민감 정보 제외)
 *   4. Mustache 템플릿에서는 Model에 DTO를 담아 렌더링
 *
 * ■ Mustache 템플릿 사용 예시
 *   Controller: model.addAttribute("reservation", SampleReservationResponse.from(entity))
 *   Template:   {{reservation.patientName}}, {{reservation.statusLabel}}
 *
 * ■ AJAX 응답 형식 (JSON)
 *   { "success": true, "data": { ... } }  — API 명세서 공통 형식 참고
 * ════════════════════════════════════════════════════════════════════════════
 */
public record SampleReservationResponse(
    Long id,
    String reservationNumber,       // 예: RES-20260315-001
    String patientName,
    String patientPhone,
    String patientEmail,
    Long departmentId,
    Long doctorId,
    LocalDate reservationDate,
    String timeSlot,                // HH:mm 형식
    String status,                  // enum name (RESERVED, RECEIVED, COMPLETED, CANCELLED)
    String statusLabel,             // 화면 표시용 한글 레이블
    LocalDateTime createdAt
) {

    // ════════════════════════════════════════════════════════════════════════
    // 정적 팩토리 메서드 — Entity → Response DTO 변환
    // ════════════════════════════════════════════════════════════════════════

    /**
     * 엔티티에서 응답 DTO 생성
     * Service 또는 Controller에서 호출: SampleReservationResponse.from(reservation)
     */
    public static SampleReservationResponse from(SampleReservation reservation) {
        return new SampleReservationResponse(
            reservation.getId(),
            reservation.getReservationNumber(),
            reservation.getPatientName(),
            reservation.getPatientPhone(),
            reservation.getPatientEmail(),
            reservation.getDepartmentId(),
            reservation.getDoctorId(),
            reservation.getReservationDate(),
            reservation.getTimeSlot(),
            reservation.getStatus().name(),
            resolveStatusLabel(reservation.getStatus()),
            reservation.getCreatedAt()
        );
    }

    /** 예약 상태 → 화면 표시용 한글 레이블 변환 */
    private static String resolveStatusLabel(SampleReservation.ReservationStatus status) {
        return switch (status) {
            case RESERVED  -> "예약 완료";
            case RECEIVED  -> "접수 완료";
            case COMPLETED -> "진료 완료";
            case CANCELLED -> "취소";
        };
    }
}
