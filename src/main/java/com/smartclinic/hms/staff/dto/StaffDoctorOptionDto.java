package com.smartclinic.hms.staff.dto;

import com.smartclinic.hms.domain.Doctor;
import lombok.Getter;

@Getter
public class StaffDoctorOptionDto {

    private final Long id;
    private final String displayName;

    public StaffDoctorOptionDto(Doctor d) {
        this.id          = d.getId();
        this.displayName = d.getDepartment().getName() + " - " + d.getStaff().getName();
    }
}
