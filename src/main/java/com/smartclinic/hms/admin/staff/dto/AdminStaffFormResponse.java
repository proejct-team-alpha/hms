package com.smartclinic.hms.admin.staff.dto;

import java.util.List;

public record AdminStaffFormResponse(
        String title,
        String formAction,
        String submitLabel,
        boolean editMode,
        Long staffId,
        String username,
        String name,
        String employeeNumber,
        String selectedRole,
        String selectedRoleLabel,
        Long selectedDepartmentId,
        boolean active,
        String retiredAt,
        String retiredAtDate,
        String retiredAtHour,
        boolean selfEdit,
        boolean employmentStatusLocked,
        boolean retiredAtLocked,
        boolean readOnly,
        boolean doctorRole,
        List<AdminStaffFormOptionResponse> roleOptions,
        List<AdminStaffDepartmentOptionResponse> departmentOptions,
        List<AdminStaffFormOptionResponse> employmentStatusOptions,
        List<AdminStaffFormOptionResponse> retiredAtHourOptions,
        List<AdminStaffFormOptionResponse> availableDayOptions
) {
}
