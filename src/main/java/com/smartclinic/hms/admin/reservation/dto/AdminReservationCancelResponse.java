package com.smartclinic.hms.admin.reservation.dto;

public record AdminReservationCancelResponse(
        boolean success,
        AdminReservationCancelDataResponse data,
        String message
) {
}
