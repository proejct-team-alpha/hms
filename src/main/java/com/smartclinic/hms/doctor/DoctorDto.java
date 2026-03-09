package com.smartclinic.hms.doctor;

// [임시] B 작업자가 정식 구현하기 전까지 A가 임시 생성. 추후 B가 교체.

import com.smartclinic.hms.domain.Doctor;
import lombok.Getter;

@Getter
public class DoctorDto {
    private final Long id;
    private final String name;

    public DoctorDto(Doctor doctor) {
        this.id = doctor.getId();
        this.name = doctor.getStaff().getName();
    }
}
