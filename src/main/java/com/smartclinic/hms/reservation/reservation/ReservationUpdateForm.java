package com.smartclinic.hms.reservation.reservation;

// [W2-#5 작업 목록]
// DONE 1. 예약 변경 폼 DTO (departmentId, doctorId, reservationDate, timeSlot)

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter @Setter
public class ReservationUpdateForm {
    private Long departmentId;
    private Long doctorId;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate reservationDate;
    private String timeSlot;
}
