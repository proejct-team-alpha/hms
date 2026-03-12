package com.smartclinic.hms.staff.walkin.dto;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WalkinRequestDto {

    private String name;
    private String phone;

    private Long doctorId;
    private Long departmentId;

    private LocalDate date;
    private String time;

    private String notes;
}
