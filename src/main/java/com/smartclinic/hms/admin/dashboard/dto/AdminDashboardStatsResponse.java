package com.smartclinic.hms.admin.dashboard.dto;

public record AdminDashboardStatsResponse(
        long todayReservations,
        long totalReservations,
        long totalStaff,
        long lowStockItems
) {
}
