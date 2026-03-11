package com.smartclinic.hms.admin.reservation.dto;

public record AdminReservationCancelResponse(
        Long reservationId,
        String status
) {
}
