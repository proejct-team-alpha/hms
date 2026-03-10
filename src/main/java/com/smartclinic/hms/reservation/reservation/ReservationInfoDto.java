package com.smartclinic.hms.reservation.reservation;

// [W2-#5.1 작업 목록]
// DONE 1. 예약 조회 결과 표시용 DTO — LazyInitializationException 방지용

import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationStatus;
import lombok.Getter;

@Getter
public class ReservationInfoDto {
    private final Long id;
    private final String reservationNumber;
    private final String patientName;
    private final String departmentName;
    private final String doctorName;
    private final String reservationDate;
    private final String timeSlot;
    private final String status;
    private final boolean canModify;

    public ReservationInfoDto(Reservation r) {
        this.id = r.getId();
        this.reservationNumber = r.getReservationNumber();
        this.patientName = r.getPatient().getName();
        this.departmentName = r.getDepartment().getName();
        this.doctorName = r.getDoctor().getStaff().getName();
        this.reservationDate = r.getReservationDate().toString();
        this.timeSlot = r.getTimeSlot();
        this.status = toKorean(r.getStatus());
        this.canModify = r.getStatus() == ReservationStatus.RESERVED;
    }

    private static String toKorean(ReservationStatus s) {
        return switch (s) {
            case RESERVED  -> "예약완료";
            case RECEIVED  -> "접수완료";
            case COMPLETED -> "진료완료";
            case CANCELLED -> "취소됨";
        };
    }
}
