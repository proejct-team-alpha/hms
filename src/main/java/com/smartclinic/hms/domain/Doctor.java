package com.smartclinic.hms.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
    private String availableDays;

    @Column(length = 100)
    private String specialty;

    public static Doctor create(Staff staff, Department department, String availableDays, String specialty) {
        Doctor doctor = new Doctor();
        doctor.staff = staff;
        doctor.department = department;
        doctor.availableDays = availableDays;
        doctor.specialty = specialty;
        return doctor;
    }

    public void updateProfile(Department department, String availableDays, String specialty) {
        this.department = department;
        this.availableDays = availableDays;
        this.specialty = specialty;
    }

    public List<DayOfWeek> getAvailableDaysList() {
        if (availableDays == null || availableDays.isBlank()) {
            return List.of(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY
            );
        }

        return Arrays.stream(availableDays.split(","))
                .map(String::trim)
                .map(day -> switch (day.toUpperCase()) {
                    case "MON" -> DayOfWeek.MONDAY;
                    case "TUE" -> DayOfWeek.TUESDAY;
                    case "WED" -> DayOfWeek.WEDNESDAY;
                    case "THU" -> DayOfWeek.THURSDAY;
                    case "FRI" -> DayOfWeek.FRIDAY;
                    case "SAT" -> DayOfWeek.SATURDAY;
                    case "SUN" -> DayOfWeek.SUNDAY;
                    default -> null;
                })
                .filter(day -> day != null)
                .collect(Collectors.toList());
    }
}
