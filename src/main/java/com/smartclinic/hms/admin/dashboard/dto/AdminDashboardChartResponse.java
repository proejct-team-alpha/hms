package com.smartclinic.hms.admin.dashboard.dto;

import java.time.LocalDate;
import java.util.List;

public record AdminDashboardChartResponse(
        List<CategoryCount> categoryCounts,
        List<DailyPatientCount> dailyPatients) {
    public record CategoryCount(
            String categoryName,
            Long totalCount) {
    }

    public record DailyPatientCount(
            LocalDate date,
            Long patientCount) {
    }
}