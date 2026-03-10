package com.smartclinic.hms.reservation.reservation;

import lombok.Getter;

// 예약 완료 화면에 필요한 정보만 담는 DTO
// 트랜잭션 종료 후 LazyInitializationException 방지용
@Getter
public class ReservationCompleteInfo {
    private final String reservationNumber;  // [W2-#5.1] 추가
    private final String patientName;
    private final String departmentName;
    private final String doctorName;
    private final String reservationDate;
    private final String timeSlot;

    public ReservationCompleteInfo(String reservationNumber, String patientName, String departmentName,
                                   String doctorName, String reservationDate, String timeSlot) {
        this.reservationNumber = reservationNumber;
        this.patientName = patientName;
        this.departmentName = departmentName;
        this.doctorName = doctorName;
        this.reservationDate = reservationDate;
        this.timeSlot = timeSlot;
    }
}
