package com.smartclinic.hms.reservation.reservation;

// [W2-#4 작업 목록]
// DONE 1. 예약 폼 DTO 생성 (name, phone, departmentId, doctorId, reservationDate, timeSlot)

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class ReservationCreateForm {
    private String name;
    private String phone;
    private Long departmentId;
    private Long doctorId;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate reservationDate;
    private String timeSlot;
}
