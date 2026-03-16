package com.smartclinic.hms.nurse.mypage;

import com.smartclinic.hms.domain.Staff;
import lombok.Getter;

@Getter
public class NurseMypageDto {

    private final String name;
    private final String username;
    private final String employeeNumber;
    private final String role;

    public NurseMypageDto(Staff staff) {
        this.name = staff.getName();
        this.username = staff.getUsername();
        this.employeeNumber = staff.getEmployeeNumber();
        this.role = "간호사";
    }
}
