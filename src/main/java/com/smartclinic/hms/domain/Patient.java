package com.smartclinic.hms.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 비회원 환자 엔티티 (ERD §2.1)
 */
@Entity
@Table(name = "patient")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(length = 255)
    private String address;

    @Column(length = 500)
    private String note;

    @Column(name = "birth_info", length = 8)
    private String birthInfo; // 생년월일 및 성별 정보 (예: "940101-2")

    @Column(name = "visit_reason", length = 500)
    private String visitReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public static Patient create(String name, String phone, String email) {
        Patient p = new Patient();
        p.name = name;
        p.phone = phone;
        p.email = email;
        return p;
    }

    public void updateInfo(String name, String phone, String email, String address, String note) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.note = note;
    }

    public void updateAddressAndNote(String address, String note) {
        this.address = address;
        this.note = note;
    }
}
