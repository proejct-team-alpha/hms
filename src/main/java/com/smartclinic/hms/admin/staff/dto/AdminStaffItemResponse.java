package com.smartclinic.hms.admin.staff.dto;

public record AdminStaffItemResponse(
        Long id,
        String name,
        String username,
        String employeeNumber,
        String role,
        String roleLabel,
        String roleBadgeClass,
        String departmentName,
        boolean active,
        String employmentStatusLabel,
        String employmentStatusBadgeClass,
        String detailUrl
) {
}
