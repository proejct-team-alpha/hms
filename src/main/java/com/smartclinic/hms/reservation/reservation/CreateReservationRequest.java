package com.smartclinic.hms.reservation.reservation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record CreateReservationRequest(
    @NotBlank(message = "이름을 입력해주세요.")
    String name,

    @NotBlank(message = "연락처를 입력해주세요.")
    @Pattern(regexp = "^$|^01[0-9][- ]?(\\d{3,4})[- ]?\\d{4}$", message = "연락처 형식이 올바르지 않습니다.")
    String phone,

    @NotNull(message = "진료과를 선택해주세요.")
    Long departmentId,

    @NotNull(message = "전문의를 선택해주세요.")
    Long doctorId,

    @NotNull(message = "예약 날짜를 선택해주세요.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate reservationDate,

    @NotBlank(message = "예약 시간을 선택해주세요.")
    @Pattern(regexp = "^$|^\\d{2}:\\d{2}$", message = "시간 형식이 올바르지 않습니다.")
    String timeSlot
) {
}
