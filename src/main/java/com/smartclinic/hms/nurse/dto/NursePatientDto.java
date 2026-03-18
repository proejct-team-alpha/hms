package com.smartclinic.hms.nurse.dto;

import com.smartclinic.hms.domain.Patient;
import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationStatus;
import lombok.Getter;

@Getter
public class NursePatientDto {

    private final Long reservationId;
    private final Long patientId;
    private final String patientName;
    private final String patientPhone;
    private final String patientEmail;
    private final String patientAddress;
    private final String patientNote;
    private final String timeSlot;
    private final String doctorName;
    private final String departmentName;
    private final String statusText;
    private final boolean canReceive;
    private final boolean canUseItem;

    public NursePatientDto(Reservation r) {
        Patient p = r.getPatient();
        this.reservationId = r.getId();
        this.patientId = p.getId();
        this.patientName = p.getName();
        this.patientPhone = p.getPhone() != null ? p.getPhone() : "";
        this.patientEmail = p.getEmail() != null ? p.getEmail() : "";
        this.patientAddress = p.getAddress() != null ? p.getAddress() : "";
        this.patientNote = p.getNote() != null ? p.getNote() : "";
        this.timeSlot = r.getTimeSlot();
        this.doctorName = r.getDoctor().getStaff().getName();
        this.departmentName = r.getDepartment().getName();
        this.statusText = toStatusText(r.getStatus());
        this.canReceive = r.getStatus() == ReservationStatus.RESERVED;
        this.canUseItem = r.getStatus() == ReservationStatus.RECEIVED
                       || r.getStatus() == ReservationStatus.IN_TREATMENT;
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
}
