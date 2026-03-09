package com.smartclinic.hms.staff.reception.dto;

import lombok.Data;

@Data
public class ReceptionUpdateRequest {
    private Long reservationId;
    private String address;
    private String note;

}
