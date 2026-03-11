package com.smartclinic.hms.admin.reservation.dto;

public record AdminReservationCancelDataResponse(
        Long reservationId,
        String status
) {
}
