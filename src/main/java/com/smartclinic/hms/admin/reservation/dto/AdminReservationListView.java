package com.smartclinic.hms.admin.reservation.dto;

import java.util.List;

public record AdminReservationListView(
        List<AdminReservationListItemView> reservations,
        List<AdminReservationStatusOption> statusOptions,
        List<AdminReservationPageLink> pageLinks,
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
