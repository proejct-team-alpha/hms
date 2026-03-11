package com.smartclinic.hms.staff.reservation.dto;

import lombok.Data;

@Data
public class PhoneReservationRequestDto {

    private String name;
    private String phone;
    private String email;

    private String department;
    private String doctor;

    private String date;
    private String time;

    private String symptoms;
    private String notes;
}
