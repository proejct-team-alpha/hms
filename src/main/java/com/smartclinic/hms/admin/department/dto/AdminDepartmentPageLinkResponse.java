package com.smartclinic.hms.admin.department.dto;

public record AdminDepartmentPageLinkResponse(
        int page,
        String url,
        boolean active
) {
}
