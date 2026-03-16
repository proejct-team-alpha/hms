package com.smartclinic.hms.staff.walkin.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WalkinRequestDto {

    @NotBlank(message = "환자 이름은 필수입니다.")
    private String name;

    @NotBlank(message = "전화번호는 필수입니다.")
    private String phone;

    @NotNull(message = "의사 선택은 필수입니다.")
    private Long doctorId;

    @NotNull(message = "진료과 선택은 필수입니다.")
    private Long departmentId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    @NotBlank(message = "접수 시간은 필수입니다.")
    private String time;

    private String notes;
}
