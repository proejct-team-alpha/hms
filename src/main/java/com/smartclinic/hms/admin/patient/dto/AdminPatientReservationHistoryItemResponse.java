package com.smartclinic.hms.admin.patient.dto;

public record AdminPatientReservationHistoryItemResponse(
        String reservationNumber,
        String reservationDate,
        String timeSlot,
        String departmentName,
        String doctorName,
        String statusText
) {
}
