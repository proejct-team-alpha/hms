package com.smartclinic.hms.admin.department;

public record AdminDepartmentPageLinkResponse(
        int page,
        String url,
        boolean active
) {
}
