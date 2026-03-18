package com.smartclinic.hms.nurse.dto;

import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationSource;
import com.smartclinic.hms.domain.ReservationStatus;
import lombok.Getter;

@Getter
public class NurseReservationDto {

    private final Long id;
    private final String reservationNumber;
    private final Long patientId;
    private final String patientName;
    private final String patientPhone;
    private final String timeSlot;
    private final String doctorName;
    private final String departmentName;
    private final String statusText;
    private final String statusBadgeClass;
    private final String sourceText;
    private final boolean canReceive;

    public NurseReservationDto(Reservation r) {
        this.id = r.getId();
        this.reservationNumber = r.getReservationNumber();
        this.patientId = r.getPatient().getId();
        this.patientName = r.getPatient().getName();
        this.patientPhone = r.getPatient().getPhone();
        this.timeSlot = r.getTimeSlot();
        this.doctorName = r.getDoctor().getStaff().getName();
        this.departmentName = r.getDepartment().getName();
        this.statusText = toStatusText(r.getStatus());
        this.statusBadgeClass = toStatusBadgeClass(r.getStatus());
        this.sourceText = toSourceText(r.getSource());
        this.canReceive = r.getStatus() == ReservationStatus.RESERVED;
    }

    private static String toStatusText(ReservationStatus s) {
        return switch (s) {
            case RESERVED -> "예약됨";
            case RECEIVED -> "진료 대기";
            case IN_TREATMENT -> "진료중";
            case COMPLETED -> "진료 완료";
            case CANCELLED -> "취소됨";
        };
    }

    private static String toStatusBadgeClass(ReservationStatus s) {
        return switch (s) {
            case RESERVED -> "bg-blue-100 text-blue-800";
            case RECEIVED -> "bg-yellow-100 text-yellow-800";
            case IN_TREATMENT -> "bg-indigo-100 text-indigo-800";
            case COMPLETED -> "bg-green-100 text-green-800";
            case CANCELLED -> "bg-slate-100 text-slate-600";
        };
    }

    private static String toSourceText(ReservationSource s) {
        return switch (s) {
            case ONLINE -> "온라인";
            case PHONE -> "전화";
            case WALKIN -> "방문";
        };
    }
}
