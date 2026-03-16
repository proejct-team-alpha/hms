package com.smartclinic.hms.staff.dto;

import com.smartclinic.hms.domain.Department;
import lombok.Getter;

@Getter
public class StaffDepartmentOptionDto {

    private final Long id;
    private final String name;

    public StaffDepartmentOptionDto(Department d) {
        this.id   = d.getId();
        this.name = d.getName();
    }
}
