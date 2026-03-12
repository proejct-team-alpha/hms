package com.smartclinic.hms.staff.reception.dto;

import lombok.Data;

@Data
public class ReceptionUpdateRequest {
    private Long reservationId;

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

}
