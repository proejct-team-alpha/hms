package com.smartclinic.hms.admin.staff.dto;

public record AdminStaffPageLinkResponse(
        int page,
        String url,
        boolean active
) {
}
