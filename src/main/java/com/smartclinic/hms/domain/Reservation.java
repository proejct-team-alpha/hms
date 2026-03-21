package com.smartclinic.hms.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 예약 엔티티 (ERD §2.5)
 *
 * <h3>취소 API 선택 가이드 (H-01)</h3>
 * <ul>
 *   <li>{@link #cancel(String)} — 접수(Staff) 화면용 <strong>부분 취소</strong>. {@code RESERVED} → 최종 {@code CANCELLED},
 *       {@code RECEIVED} → 접수 롤백 {@code RESERVED}. {@code IN_TREATMENT}에서는 호출 불가(명시적 예외).</li>
 *   <li>{@link #cancelFully(String)} — 비회원 취소·예약 변경 시 구예약 폐기 등 <strong>무조건 CANCELLED</strong> 전이.
 *       {@link com.smartclinic.hms.reservation.reservation.ReservationService} 에서 사용.</li>
 * </ul>
 */
@Entity
@Table(name = "reservation",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_reservation_doctor_date_slot",
        columnNames = {"doctor_id", "reservation_date", "time_slot"}
    ))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_number", nullable = false, unique = true, length = 25)
    private String reservationNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "reservation_date", nullable = false)
    private LocalDate reservationDate;

    @Column(name = "time_slot", nullable = false, length = 10)
    private String timeSlot;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationSource source;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = ReservationStatus.RESERVED;
        if (this.source == null) this.source = ReservationSource.ONLINE;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public static Reservation create(String reservationNumber, Patient patient, Doctor doctor,
                                    Department department, LocalDate reservationDate, String timeSlot,
                                    ReservationSource source) {
        Reservation r = new Reservation();
        r.reservationNumber = reservationNumber;
        r.patient = patient;
        r.doctor = doctor;
        r.department = department;
        r.reservationDate = reservationDate;
        r.timeSlot = timeSlot;
        r.source = source;
        r.status = ReservationStatus.RESERVED; // 기본 상태 명시적 초기화
        return r;
    }

    public void receive() {
        if (this.status != ReservationStatus.RESERVED) {
            throw new IllegalStateException("RESERVED 상태에서만 접수 가능. 현재: " + this.status);
        }
        this.status = ReservationStatus.RECEIVED;
    }

    public void startTreatment() {
        if (this.status != ReservationStatus.RECEIVED) {
            throw new IllegalStateException("RECEIVED 상태에서만 진료 시작 가능. 현재: " + this.status);
        }
        this.status = ReservationStatus.IN_TREATMENT;
    }

    public void complete() {
        if (this.status != ReservationStatus.RECEIVED && this.status != ReservationStatus.IN_TREATMENT) {
            throw new IllegalStateException("RECEIVED 또는 IN_TREATMENT 상태에서만 진료완료 가능. 현재: " + this.status);
        }
        this.status = ReservationStatus.COMPLETED;
    }

    public void cancel() {
        this.cancel(null);
    }

    public void cancel(String reason) {
        if (this.status == ReservationStatus.COMPLETED) {
            throw new IllegalStateException("진료 완료된 예약은 취소 불가");
        }
        if (this.status == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 예약");
        }
        if (this.status == ReservationStatus.IN_TREATMENT) {
            throw new IllegalStateException(
                    "진료 중인 예약은 접수 화면 취소로 되돌릴 수 없습니다. 진료 완료 처리 또는 관리자 취소를 사용하세요.");
        }

        if (this.status == ReservationStatus.RECEIVED) {
            // 진료 대기 상태에서 취소하면 접수 대기 상태로 되돌림
            this.status = ReservationStatus.RESERVED;
        } else if (this.status == ReservationStatus.RESERVED) {
            // 접수 대기 상태에서 취소하면 최종 취소 상태로 변경
            this.status = ReservationStatus.CANCELLED;
            this.cancellationReason = reason;
        }
    }

    /**
     * 현재 상태와 무관하게 예약을 즉시 CANCELLED로 전이한다.
     * 비회원 취소, 예약 변경(구 예약 폐기) 등 상태 단계와 무관하게 최종 취소가 필요한 경우에 사용한다.
     */
    public void cancelFully(String reason) {
        if (this.status == ReservationStatus.COMPLETED) {
            throw new IllegalStateException("진료 완료된 예약은 취소 불가");
        }
        if (this.status == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 예약");
        }
        this.status = ReservationStatus.CANCELLED;
        this.cancellationReason = reason;
    }
}
