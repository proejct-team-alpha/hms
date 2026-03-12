package com.smartclinic.hms.reservation.reservation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 예약 변경 폼 DTO
 * 프로젝트 규칙(rule_spring.md §6)에 따라 record 사용
 */
public record ReservationUpdateForm(
    @NotNull(message = "진료과를 선택해주세요.")
    Long departmentId,

    @NotNull(message = "전문의를 선택해주세요.")
    Long doctorId,

    @NotNull(message = "예약 날짜를 선택해주세요.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate reservationDate,

    @NotBlank(message = "예약 시간을 선택해주세요.")
    String timeSlot
) {
}
