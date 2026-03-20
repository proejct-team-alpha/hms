package com.smartclinic.hms.admin.patient.dto;

import java.util.List;

public record AdminPatientDetailResponse(
        Long patientId,
        String name,
        String phone,
        String email,
        String address,
        String note,
        String noteValue,
        List<AdminPatientReservationHistoryItemResponse> reservationHistories
) {
}
