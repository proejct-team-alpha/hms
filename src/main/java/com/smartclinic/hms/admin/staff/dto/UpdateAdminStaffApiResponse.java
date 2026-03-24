package com.smartclinic.hms.admin.staff.dto;

import java.util.List;

public record UpdateAdminStaffApiResponse(
        Long staffId,
        String username,
        String employeeNumber,
        String name,
        String role,
        Long departmentId,
        String departmentName,
        boolean active,
        List<String> availableDays,
        String message
) {
}