package com.smartclinic.hms.admin.patient.dto;

import com.smartclinic.hms.admin.patient.AdminPatientSummary;
import java.util.List;

public record AdminPatientListResponse(
        List<AdminPatientSummary> patients,
        List<AdminPatientPageLinkResponse> pageLinks,
        String nameKeyword,
        String contactKeyword,
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
