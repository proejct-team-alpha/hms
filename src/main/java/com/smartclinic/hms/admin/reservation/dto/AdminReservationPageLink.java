package com.smartclinic.hms.admin.reservation.dto;

public record AdminReservationPageLink(
        int page,
        String url,
        boolean active
) {
}
