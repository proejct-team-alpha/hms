package com.smartclinic.hms.admin.reservation.dto;

public record AdminReservationStatusOption(
        String value,
        String label,
        String url,
        boolean selected
) {
}
