package com.smartclinic.hms.admin.department;

import com.smartclinic.hms.domain.Department;

public record AdminDepartmentDetailResponse(
        Long departmentId,
        String name,
        boolean active,
        String activeText,
        String activeBadgeClass,
        boolean activatable,
        boolean deactivatable,
        String updateAction,
        String activateAction,
        String deactivateAction
) {

    public static AdminDepartmentDetailResponse from(Department department) {
        boolean active = department.isActive();

        return new AdminDepartmentDetailResponse(
                department.getId(),
                department.getName(),
                active,
                active ? "운영 중" : "비운영",
                active
                        ? "px-2 py-1 text-xs font-medium rounded-full bg-green-100 text-green-700"
                        : "px-2 py-1 text-xs font-medium rounded-full bg-red-100 text-red-700",
                !active,
                active,
                "/admin/department/update",
                "/admin/department/activate",
                "/admin/department/deactivate"
        );
    }
}
