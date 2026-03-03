package com.smartclinic.hms.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 내부 직원 엔티티 (ERD §2.3)
 */
@Entity
@Table(name = "staff")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "employee_number", nullable = false, unique = true, length = 20)
    private String employeeNumber;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StaffRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public static Staff create(String username, String employeeNumber, String encodedPassword,
                               String name, StaffRole role, Department department) {
        Staff s = new Staff();
        s.username = username;
        s.employeeNumber = employeeNumber;
        s.password = encodedPassword;
        s.name = name;
        s.role = role;
        s.department = department;
        return s;
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void update(String name, Department department, boolean active) {
        this.name = name;
        this.department = department;
        this.active = active;
    }
}
