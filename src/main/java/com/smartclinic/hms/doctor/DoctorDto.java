package com.smartclinic.hms.doctor;

// [임시] B 작업자가 정식 구현하기 전까지 A가 임시 생성. 추후 B가 교체.
// [W2-#3 작업 목록]
// DONE 1. availableDays 필드 추가
// DONE 2. 생성자에서 doctor.getAvailableDays() 매핑

import com.smartclinic.hms.domain.Doctor;
import lombok.Getter;

@Getter
public class DoctorDto {
    private final Long id;
    private final String name;
    private final String availableDays;

    public DoctorDto(Doctor doctor) {
        this.id = doctor.getId();
        this.name = doctor.getStaff().getName();
        this.availableDays = doctor.getAvailableDays();
    }
}
