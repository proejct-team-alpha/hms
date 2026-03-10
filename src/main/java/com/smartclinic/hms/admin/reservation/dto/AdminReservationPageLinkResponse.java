package com.smartclinic.hms.admin.reservation.dto;

public record AdminReservationPageLinkResponse(
        int page,
        String url,
        boolean active
) {
}
