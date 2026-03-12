package com.smartclinic.hms.staff.reservation.dto;

import lombok.Data;

@Data
public class PhoneReservationRequestDto {

    private String name;
    private String phone;
    private String email;

    private Long departmentId;
    private Long doctorId;

    private String date;
    private String time;

    private String symptoms;
    private String notes;
}
