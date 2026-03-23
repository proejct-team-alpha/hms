package com.smartclinic.hms.staff.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PhoneReservationRequestDto {

    @NotBlank(message = "환자 이름은 필수입니다.")
    private String name;

    @NotBlank(message = "전화번호는 필수입니다.")
    @Pattern(regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$", message = "올바른 전화번호 형식이 아닙니다.")
    private String phone;

    private String birthInfo; // [기능 구현] 주민번호 필드 추가 (YYMMDD-G)
    private String email;

    @NotNull(message = "진료과 선택은 필수입니다.")
    private Long departmentId;

    @NotNull(message = "의사 선택은 필수입니다.")
    private Long doctorId;

    @NotBlank(message = "예약 날짜는 필수입니다.")
    private String date;

    @NotBlank(message = "예약 시간은 필수입니다.")
    private String time;

    private String visitReason; // [기능 구현] symptoms -> visitReason 으로 명칭 통합
    private String notes;
}
