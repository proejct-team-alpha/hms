package com.smartclinic.hms.admin.department;

import com.smartclinic.hms.domain.Department;
import lombok.Getter;

@Getter
public class AdminDepartmentDto {

    private final Long id;
    private final String name;
    private final boolean active;
    private final String activeText;
    private final String activeBadgeClass;

    public AdminDepartmentDto(Department dept) {
        this.id = dept.getId();
        this.name = dept.getName();
        this.active = dept.isActive();
        this.activeText = dept.isActive() ? "운영 중" : "비운영";
        this.activeBadgeClass = dept.isActive()
                ? "px-2 py-1 text-xs font-medium rounded-full bg-green-100 text-green-700"
                : "px-2 py-1 text-xs font-medium rounded-full bg-red-100 text-red-700";
    }
}
