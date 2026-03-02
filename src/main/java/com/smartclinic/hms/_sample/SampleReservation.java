package com.smartclinic.hms._sample;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ════════════════════════════════════════════════════════════════════════════
 * [샘플] SampleReservation — JPA Entity 작성 가이드
 * ════════════════════════════════════════════════════════════════════════════
 *
 * ■ 패키지 위치 (실제 구현 시)
 *   com.smartclinic.hms.domain.Reservation
 *
 * ■ 엔티티 작성 핵심 규칙
 *   1. @NoArgsConstructor(access = PROTECTED) — JPA 기본 생성자는 protected
 *   2. 외부에서 new 로 직접 생성 금지 → 정적 팩토리 메서드(create) 사용
 *   3. Setter 사용 금지 → 도메인 메서드(상태 전이 등)로만 필드 변경
 *   4. @Enumerated(EnumType.STRING) — 열거형은 반드시 STRING 방식 저장
 *   5. @PrePersist / @PreUpdate — 생성·수정 시각 자동 관리
 *   6. 비즈니스 규칙(상태 전이 검증 등)을 엔티티 내부 메서드에 작성
 *
 * ■ 연관 테이블 (실제 도메인 엔티티 참조)
 *   PATIENT, DOCTOR, DEPARTMENT, LLM_RECOMMENDATION, TREATMENT_RECORD
 * ════════════════════════════════════════════════════════════════════════════
 */
@Entity
@Table(
    name = "sample_reservation",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_sample_res_doctor_date_slot",
            columnNames = {"doctor_id", "reservation_date", "time_slot"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA 기본 생성자 — 외부 접근 차단
@ToString(exclude = {})
public class SampleReservation {

    // ── PK ──────────────────────────────────────────────────────────────────
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── 고유 예약번호 ─────────────────────────────────────────────────────
    @Column(nullable = false, unique = true, length = 30)
    private String reservationNumber;   // 예: RES-20260315-001

    // ── 환자 정보 (비회원 예약 시 직접 저장) ──────────────────────────────
    // 실제 구현에서는 @ManyToOne Patient 엔티티로 대체
    @Column(nullable = false, length = 20)
    private String patientName;

    @Column(nullable = false, length = 20)
    private String patientPhone;

    @Column(length = 100)
    private String patientEmail;        // 선택 필드

    // ── 진료과·의사 FK (실제 구현 시 @ManyToOne 으로 변경) ────────────────
    // 실제 구현:
    //   @ManyToOne(fetch = FetchType.LAZY)
    //   @JoinColumn(name = "department_id", nullable = false)
    //   private Department department;
    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    // ── 예약 일시 ─────────────────────────────────────────────────────────
    @Column(nullable = false)
    private LocalDate reservationDate;

    @Column(nullable = false, length = 5)
    private String timeSlot;            // HH:mm (30분 단위 고정 슬롯, 예: "09:00")

    // ── 예약 상태 ─────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)        // 반드시 STRING — 순서 의존(ORDINAL) 금지
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    // ── LLM 추천 연결 (선택) ──────────────────────────────────────────────
    // 실제 구현에서는 @ManyToOne LlmRecommendation 으로 대체
    @Column(name = "llm_recommendation_id")
    private Long llmRecommendationId;   // null 허용 — 직접 예약 시 null

    // ── 감사 필드 (Audit Fields) ──────────────────────────────────────────
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // ── JPA 콜백 ──────────────────────────────────────────────────────────
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ReservationStatus.RESERVED;   // 기본값: 예약 상태
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ════════════════════════════════════════════════════════════════════════
    // 정적 팩토리 메서드 — 외부에서 엔티티 생성 시 이 메서드만 사용
    // ════════════════════════════════════════════════════════════════════════
    public static SampleReservation create(
            String reservationNumber,
            String patientName,
            String patientPhone,
            String patientEmail,
            Long departmentId,
            Long doctorId,
            LocalDate reservationDate,
            String timeSlot,
            Long llmRecommendationId) {

        SampleReservation reservation = new SampleReservation();
        reservation.reservationNumber    = reservationNumber;
        reservation.patientName          = patientName;
        reservation.patientPhone         = patientPhone;
        reservation.patientEmail         = patientEmail;
        reservation.departmentId         = departmentId;
        reservation.doctorId             = doctorId;
        reservation.reservationDate      = reservationDate;
        reservation.timeSlot             = timeSlot;
        reservation.llmRecommendationId  = llmRecommendationId;
        return reservation;
    }

    // ════════════════════════════════════════════════════════════════════════
    // 도메인 메서드 — 상태 전이 (비즈니스 규칙 포함)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * 접수 처리 — RESERVED → RECEIVED
     * 접수 직원(ROLE_STAFF)이 처리
     */
    public void receive() {
        if (this.status != ReservationStatus.RESERVED) {
            throw new IllegalStateException("RESERVED 상태에서만 접수 처리가 가능합니다. 현재 상태: " + this.status);
        }
        this.status = ReservationStatus.RECEIVED;
    }

    /**
     * 진료 완료 — RECEIVED → COMPLETED
     * 의사(ROLE_DOCTOR)가 처리
     */
    public void complete() {
        if (this.status != ReservationStatus.RECEIVED) {
            throw new IllegalStateException("RECEIVED 상태에서만 진료 완료 처리가 가능합니다. 현재 상태: " + this.status);
        }
        this.status = ReservationStatus.COMPLETED;
    }

    /**
     * 예약 취소 — RESERVED·RECEIVED → CANCELLED
     * 관리자(ROLE_ADMIN)만 처리 가능
     * COMPLETED 상태에서는 취소 불가
     */
    public void cancel() {
        if (this.status == ReservationStatus.COMPLETED) {
            throw new IllegalStateException("진료가 완료된 예약은 취소할 수 없습니다.");
        }
        if (this.status == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 예약입니다.");
        }
        this.status = ReservationStatus.CANCELLED;
    }

    // ════════════════════════════════════════════════════════════════════════
    // 상태 열거형 — 예약의 전체 생명주기
    // ════════════════════════════════════════════════════════════════════════
    public enum ReservationStatus {
        RESERVED,       // 예약 완료 (비회원 온라인 예약, 전화 예약)
        RECEIVED,       // 접수 완료 (당일 창구 접수 처리 후)
        COMPLETED,      // 진료 완료 (의사 진료 기록 저장 후)
        CANCELLED       // 취소
    }
}
