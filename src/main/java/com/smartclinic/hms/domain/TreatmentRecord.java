package com.smartclinic.hms.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 진료 기록 엔티티 (ERD §2.6)
 */
@Entity
@Table(name = "treatment_record")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TreatmentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String diagnosis;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String prescription;

    @Column(columnDefinition = "TEXT")
    private String remark;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void update(String diagnosis, String prescription, String remark) {
        this.diagnosis = diagnosis;
        this.prescription = prescription;
        this.remark = remark != null ? remark : "";
    }

    public static TreatmentRecord create(Reservation reservation, Doctor doctor,
                                        String diagnosis, String prescription, String remark) {
        TreatmentRecord tr = new TreatmentRecord();
        tr.reservation = reservation;
        tr.doctor = doctor;
        tr.diagnosis = diagnosis;
        tr.prescription = prescription;
        tr.remark = remark != null ? remark : "";
        return tr;
    }
}
