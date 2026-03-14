package com.smartclinic.hms.staff.walkin.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WalkinRequestDto {

    private String name;
    private String phone;

    private Long doctorId;
    private Long departmentId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;
    private String time;

    private String notes;
}
