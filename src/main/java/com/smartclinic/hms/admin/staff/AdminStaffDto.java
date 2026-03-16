package com.smartclinic.hms.admin.staff;

import com.smartclinic.hms.domain.Staff;
import com.smartclinic.hms.domain.StaffRole;
import lombok.Getter;

@Getter
public class AdminStaffDto {

    private final Long id;
    private final String name;
    private final String username;
    private final String employeeNumber;
    private final String roleText;
    private final String departmentName;
    private final boolean active;
    private final String activeText;
    private final String activeBadgeClass;

    public AdminStaffDto(Staff staff) {
        this.id = staff.getId();
        this.name = staff.getName();
        this.username = staff.getUsername();
        this.employeeNumber = staff.getEmployeeNumber();
        this.roleText = toRoleText(staff.getRole());
        this.departmentName = staff.getDepartment() != null ? staff.getDepartment().getName() : "-";
        this.active = staff.isActive();
        this.activeText = staff.isActive() ? "재직 중" : "퇴직";
        this.activeBadgeClass = staff.isActive()
                ? "px-2 py-1 text-xs font-medium rounded-full bg-green-100 text-green-700"
                : "px-2 py-1 text-xs font-medium rounded-full bg-red-100 text-red-700";
    }

    private String toRoleText(StaffRole role) {
        return switch (role) {
            case ADMIN -> "관리자";
            case DOCTOR -> "의사";
            case NURSE -> "간호사";
            case STAFF -> "원무과";
            case ITEM_MANAGER -> "물품관리";
        };
    }
}
