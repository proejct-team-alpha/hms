package com.smartclinic.hms.admin.department;

import java.util.List;

public record AdminDepartmentListResponse(
        List<AdminDepartmentDto> departments,
        List<AdminDepartmentPageLinkResponse> pageLinks,
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
