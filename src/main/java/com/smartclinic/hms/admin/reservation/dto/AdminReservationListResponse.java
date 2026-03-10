package com.smartclinic.hms.admin.reservation.dto;

import java.util.List;

public record AdminReservationListResponse(
        List<AdminReservationItemResponse> reservations,
        List<AdminReservationStatusOptionResponse> statusOptions,
        List<AdminReservationPageLinkResponse> pageLinks,
        String selectedStatus,
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
