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

    /**
     * 접수 시각 (환자가 실제 내원하여 접수 처리가 완료된 시점)
     */
    @Column(name = "reception_time")
    private LocalDateTime receptionTime;

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

    @Column(name = "treatment_completed", nullable = false, columnDefinition = "boolean default false")
    private boolean treatmentCompleted = false; // 간호사 처치 완료 여부

    @Column(name = "is_paid", nullable = false, columnDefinition = "boolean default false")
    private boolean paid = false; // 수납 완료 여부

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = ReservationStatus.RESERVED;
        if (this.source == null) this.source = ReservationSource.ONLINE;
        this.treatmentCompleted = false;
        this.paid = false;
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
        r.treatmentCompleted = false;
        r.paid = false;
        return r;
    }

    /**
     * 예약을 실제 접수(RECEIVED) 상태로 전이시키고 접수 시각을 기록한다.
     * Staff가 '접수' 버튼을 눌러 환자의 내원을 확인한 시점에 호출된다.
     */
    public void receive() {
        checkPaid();
        if (this.status != ReservationStatus.RESERVED) {
            throw new IllegalStateException("RESERVED 상태에서만 접수 가능. 현재: " + this.status);
        }
        this.status = ReservationStatus.RECEIVED;
        this.receptionTime = LocalDateTime.now(); // 접수 버튼을 누른 현재 시각을 기록
    }

    public void startTreatment() {
        checkPaid();
        if (this.status != ReservationStatus.RECEIVED) {
            throw new IllegalStateException("RECEIVED 상태에서만 진료 시작 가능. 현재: " + this.status);
        }
        this.status = ReservationStatus.IN_TREATMENT;
    }

    public void complete() {
        checkPaid();
        if (this.status != ReservationStatus.RECEIVED && this.status != ReservationStatus.IN_TREATMENT) {
            throw new IllegalStateException("RECEIVED 또는 IN_TREATMENT 상태에서만 진료완료 가능. 현재: " + this.status);
        }
        this.status = ReservationStatus.COMPLETED;
    }

    /**
     * 간호사가 처치를 완료했을 때 호출 (처치 완료 여부 플래그를 true로 변경)
     */
    public void completeTreatment() {
        checkPaid();
        if (this.status != ReservationStatus.COMPLETED) {
            throw new IllegalStateException("진료 완료(COMPLETED) 상태에서만 처치 완료 처리 가능. 현재: " + this.status);
        }
        this.treatmentCompleted = true;
    }

    /**
     * 수납이 완료되었을 때 호출 (수납 완료 여부 플래그를 true로 변경)
     * 이 상태가 되면 더 이상 어떠한 상태 변경도 불가능함.
     */
    public void pay() {
        if (!this.treatmentCompleted) {
            throw new IllegalStateException("처치 완료(treatmentCompleted) 상태에서만 수납 처리 가능합니다.");
        }
        this.paid = true;
    }

    /**
     * 수납 완료 상태인지 확인하여 상태 변경을 차단
     */
    private void checkPaid() {
        if (this.paid) {
            throw new IllegalStateException("이미 수납 완료된 예약은 수정할 수 없습니다.");
        }
    }

    public void cancel() {
        this.cancel(null);
    }

    public void cancel(String reason) {
        checkPaid();
        if (this.status == ReservationStatus.COMPLETED || this.treatmentCompleted) {
            throw new IllegalStateException("진료 또는 처치가 완료된 예약은 취소 불가. 현재 상태: " + this.status + ", 처치완료여부: " + this.treatmentCompleted);
        }
        if (this.status == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 예약");
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
        checkPaid();
        if (this.status == ReservationStatus.COMPLETED || this.treatmentCompleted) {
            throw new IllegalStateException("진료 또는 처치가 완료된 예약은 취소 불가. 현재 상태: " + this.status + ", 처치완료여부: " + this.treatmentCompleted);
        }
        if (this.status == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 예약");
        }
        this.status = ReservationStatus.CANCELLED;
        this.cancellationReason = reason;
    }
}
