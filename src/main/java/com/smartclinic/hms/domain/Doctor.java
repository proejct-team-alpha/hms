package com.smartclinic.hms.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 의사 상세 정보 엔티티 (ERD §2.4)
 */
@Entity
@Table(name = "doctor")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false, unique = true)
    private Staff staff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "available_days", length = 50)
    private String availableDays;  // "MON,TUE,WED" 형식

    @Column(length = 100)
    private String specialty;

    public static Doctor create(Staff staff, Department department, String availableDays, String specialty) {
        Doctor d = new Doctor();
        d.staff = staff;
        d.department = department;
        d.availableDays = availableDays;
        d.specialty = specialty;
        return d;
    }

    /**
     * available_days 문자열을 DayOfWeek 리스트로 변환.
     * NULL이면 평일(MON~FRI) 전체 가능으로 간주.
     */
    public List<java.time.DayOfWeek> getAvailableDaysList() {
        if (availableDays == null || availableDays.isBlank()) {
            return List.of(
                java.time.DayOfWeek.MONDAY,
                java.time.DayOfWeek.TUESDAY,
                java.time.DayOfWeek.WEDNESDAY,
                java.time.DayOfWeek.THURSDAY,
                java.time.DayOfWeek.FRIDAY
            );
        }
        return Arrays.stream(availableDays.split(","))
            .map(String::trim)
            .map(s -> switch (s.toUpperCase()) {
                case "MON" -> java.time.DayOfWeek.MONDAY;
                case "TUE" -> java.time.DayOfWeek.TUESDAY;
                case "WED" -> java.time.DayOfWeek.WEDNESDAY;
                case "THU" -> java.time.DayOfWeek.THURSDAY;
                case "FRI" -> java.time.DayOfWeek.FRIDAY;
                case "SAT" -> java.time.DayOfWeek.SATURDAY;
                case "SUN" -> java.time.DayOfWeek.SUNDAY;
                default -> null;
            })
            .filter(d -> d != null)
            .collect(Collectors.toList());
    }
}
