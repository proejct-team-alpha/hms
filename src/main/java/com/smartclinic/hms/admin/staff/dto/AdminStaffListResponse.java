package com.smartclinic.hms.admin.staff.dto;

import java.util.List;

public record AdminStaffListResponse(
        List<AdminStaffItemResponse> staffs,
        List<AdminStaffFilterOptionResponse> roleOptions,
        List<AdminStaffFilterOptionResponse> employmentStatusOptions,
        List<AdminStaffPageLinkResponse> pageLinks,
        String keyword,
        String selectedRole,
        String selectedEmploymentStatus,
        long totalCount,
        int currentPage,
        int size,
        int totalPages,
        boolean hasPrevious,
        boolean hasNext,
        String previousUrl,
        String nextUrl
) {
}
