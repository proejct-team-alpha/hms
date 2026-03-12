package com.smartclinic.hms.reservation.reservation;

// [W2-#4 작업 목록]
// DONE 1. 예약 폼 DTO 생성 (name, phone, departmentId, doctorId, reservationDate, timeSlot)

// [W2-#8 작업 목록]
// DONE 1. @Valid 유효성 검증 어노테이션 추가

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class ReservationCreateForm {
    @NotBlank(message = "이름을 입력해주세요.")
    private String name;

    @NotBlank(message = "연락처를 입력해주세요.")
    private String phone;

    @NotNull(message = "진료과를 선택해주세요.")
    private Long departmentId;

    @NotNull(message = "전문의를 선택해주세요.")
    private Long doctorId;

    @NotNull(message = "예약 날짜를 선택해주세요.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate reservationDate;

    @NotBlank(message = "예약 시간을 선택해주세요.")
    private String timeSlot;
}
