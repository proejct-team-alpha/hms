package com.smartclinic.hms.admin.dashboard.dto;

import java.time.LocalDate;
import java.util.List;

public record AdminDashboardChartResponse(
        List<ItemFlowDay> itemFlowDays,
        List<DailyPatientCount> dailyPatients) {

    public record ItemFlowDay(
            String label,
            int inAmount,
            int outAmount,
            int inHeight,
            int outHeight) {
    }

    public record DailyPatientCount(
            LocalDate date,
            Long patientCount) {
    }
}