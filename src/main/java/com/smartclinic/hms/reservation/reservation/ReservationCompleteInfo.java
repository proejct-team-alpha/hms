package com.smartclinic.hms.reservation.reservation;

// 예약 완료 화면에 필요한 정보만 담는 DTO (flash attribute → Mustache {{#info}} 바인딩)
// 트랜잭션 종료 후 LazyInitializationException 방지용
public record ReservationCompleteInfo(
        String reservationNumber,
        String patientName,
        String departmentName,
        String doctorName,
        String reservationDate,
        String timeSlot
) {
}
