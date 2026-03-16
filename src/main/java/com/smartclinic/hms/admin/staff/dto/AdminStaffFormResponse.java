package com.smartclinic.hms.admin.staff.dto;

import java.util.List;

public record AdminStaffFormResponse(
        String title,
        String formAction,
        String submitLabel,
        String username,
        String name,
        String employeeNumber,
        String selectedRole,
        Long selectedDepartmentId,
        boolean active,
        List<AdminStaffFormOptionResponse> roleOptions,
        List<AdminStaffDepartmentOptionResponse> departmentOptions,
        List<AdminStaffFormOptionResponse> employmentStatusOptions
) {
}
