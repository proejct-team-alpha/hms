package com.smartclinic.hms.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 병원 규칙 엔티티 (ERD §2.8)
 */
@Entity
@Table(name = "hospital_rule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HospitalRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private HospitalRuleCategory category;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(length = 100)
    private String target;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public static HospitalRule create(String title, String content, HospitalRuleCategory category, boolean active) {
        HospitalRule r = new HospitalRule();
        r.title = title;
        r.content = content;
        r.category = category;
        r.active = active;
        return r;
    }

    public void update(String title, String content, HospitalRuleCategory category, boolean active) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.active = active;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
