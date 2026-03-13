package com.smartclinic.hms.item.mypage;

import com.smartclinic.hms.domain.Staff;
import lombok.Getter;

@Getter
public class ItemManagerMypageDto {

    private final String name;
    private final String username;
    private final String employeeNumber;
    private final String role;

    public ItemManagerMypageDto(Staff staff) {
        this.name = staff.getName();
        this.username = staff.getUsername();
        this.employeeNumber = staff.getEmployeeNumber();
        this.role = "물품 담당자";
    }
}
