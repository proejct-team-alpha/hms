package com.smartclinic.hms.staff.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PhoneReservationRequestDto {

    @NotBlank(message = "환자 이름은 필수입니다.")
    private String name;

    @NotBlank(message = "환자 이름은 필수입니다.")
    private String phone;

    private String email;

    @NotNull(message = "진료과 선택은 필수입니다.")
    private Long departmentId;

    @NotNull(message = "의사 선택은 필수입니다.")
    private Long doctorId;

    @NotBlank(message = "예약 날짜는 필수입니다.")
    private String date;

    @NotBlank(message = "예약 날짜는 필수입니다.")
    private String time;

    private String symptoms;
    private String notes;
}
