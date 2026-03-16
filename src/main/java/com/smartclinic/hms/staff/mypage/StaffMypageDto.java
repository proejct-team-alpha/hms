package com.smartclinic.hms.staff.mypage;

import com.smartclinic.hms.domain.Staff;
import lombok.Getter;

@Getter
public class StaffMypageDto {

    private final String name;
    private final String username;
    private final String employeeNumber;
    private final String role;

    public StaffMypageDto(Staff staff) {
        this.name           = staff.getName();
        this.username       = staff.getUsername();
        this.employeeNumber = staff.getEmployeeNumber();
        this.role           = "원무과 직원";
    }
}
