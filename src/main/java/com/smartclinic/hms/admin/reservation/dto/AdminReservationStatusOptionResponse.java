package com.smartclinic.hms.admin.reservation.dto;

public record AdminReservationStatusOptionResponse(
        String value,
        String label,
        String url,
        boolean selected
) {
}
