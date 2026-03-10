package com.smartclinic.hms.admin.reservation.dto;

public record AdminReservationItemResponse(
        Long id,
        String reservationNumber,
        String reservationDate,
        String timeSlot,
        String patientName,
        String patientPhone,
        String departmentName,
        String doctorName,
        String status,
        String statusLabel,
        boolean reserved,
        boolean received,
        boolean completed,
        boolean cancelled
) {
}
