package com.smartclinic.hms.reservation.reservation;

// [W2-#5 작업 목록]
// DONE 1. 진료과 API 응답용 DTO (id, name)

import com.smartclinic.hms.domain.Department;
import lombok.Getter;

@Getter
public class DepartmentDto {
    private final Long id;
    private final String name;

    public DepartmentDto(Department department) {
        this.id = department.getId();
        this.name = department.getName();
    }
}
