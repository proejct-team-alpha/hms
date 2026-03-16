package com.smartclinic.hms.staff.mypage;

import com.smartclinic.hms.domain.Staff;
import lombok.Getter;

@Getter
public class StaffMypageDto {

    private final String name;
    private final String username;
    private final String employeeNumber;
    private final String role;
    private final String email;
    private final String phone;

    public StaffMypageDto(Staff staff) {
        this.name           = staff.getName();
        this.username       = staff.getUsername();
        this.employeeNumber = staff.getEmployeeNumber();
        this.role           = "원무과 직원";
        this.email          = staff.getEmail() != null ? staff.getEmail() : "";
        this.phone          = staff.getPhone() != null ? staff.getPhone() : "";
    }
}
