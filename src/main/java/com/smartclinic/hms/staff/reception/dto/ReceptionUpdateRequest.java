package com.smartclinic.hms.staff.reception.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReceptionUpdateRequest {

    @NotNull(message = "예약 ID는 필수입니다.")
    private Long reservationId;

    // public Long getReservationId() {
    // return reservationId;
    // }

    // public void setReservationId(Long reservationId) {
    // this.reservationId = reservationId;
    // }

}
