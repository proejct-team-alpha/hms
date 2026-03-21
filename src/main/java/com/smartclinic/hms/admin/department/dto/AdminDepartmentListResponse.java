package com.smartclinic.hms.admin.department.dto;

import java.util.List;

public record AdminDepartmentListResponse(
        List<AdminDepartmentItemResponse> departments,
        List<AdminDepartmentPageLinkResponse> pageLinks,
        long totalCount,
        int currentPage,
        int size,
        int totalPages,
        boolean hasPages,
        boolean hasPrevious,
        boolean hasNext,
        String previousUrl,
        String nextUrl
) {
}
