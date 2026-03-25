package com.smartclinic.hms.admin.reservation.dto;

// 관리자 예약화면 page DTO
public record AdminReservationPageLinkResponse(
                int page,
                String url,
                boolean active) {
}
