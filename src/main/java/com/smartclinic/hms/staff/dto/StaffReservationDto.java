package com.smartclinic.hms.staff.dto;

import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationStatus;
import com.smartclinic.hms.domain.ReservationSource;
import lombok.Getter;

@Getter
public class StaffReservationDto {

    private final Long id;
    private final String reservationNumber;
    private final String patientName;
    private final String patientPhone;
    private final String patientEmail;
    private final String patientAddress;
    private final String patientNote;
    private final String reservationDate;
    private final String timeSlot;
    private final String departmentName;
    private final String doctorName;
    private final String statusText;
    private final String statusBadgeClass;
    private final String sourceText;
    private final String sourceBadgeClass;
    private final boolean canReceive;
    private final boolean canCancel;

    public StaffReservationDto(Reservation r) {
        this.id = r.getId();
        this.reservationNumber = r.getReservationNumber();
        this.patientName = r.getPatient().getName();
        this.patientPhone = r.getPatient().getPhone();
        this.patientEmail = r.getPatient().getEmail() != null ? r.getPatient().getEmail() : "-";
        this.patientAddress = r.getPatient().getAddress() != null ? r.getPatient().getAddress() : "";
        this.patientNote = r.getPatient().getNote() != null ? r.getPatient().getNote() : "";
        this.reservationDate = r.getReservationDate().toString();
        this.timeSlot = r.getTimeSlot();
        this.departmentName = r.getDepartment().getName();
        this.doctorName = r.getDoctor().getStaff().getName();

        this.statusText = switch (r.getStatus()) {
            case RESERVED -> "접수 대기";
            case RECEIVED -> "진료 대기";
            case COMPLETED -> "진료 완료";
            case CANCELLED -> "취소됨";
        };
        this.statusBadgeClass = switch (r.getStatus()) {
            case RESERVED -> "bg-indigo-100 text-indigo-700";
            case RECEIVED -> "bg-yellow-100 text-yellow-700";
            case COMPLETED -> "bg-green-100 text-green-700";
            case CANCELLED -> "bg-slate-100 text-slate-500";
        };
        this.sourceText = switch (r.getSource()) {
            case ONLINE -> "온라인";
            case PHONE -> "전화";
            case WALKIN -> "방문";
        };
        this.sourceBadgeClass = switch (r.getSource()) {
            case ONLINE -> "bg-blue-100 text-blue-800";
            case PHONE -> "bg-purple-100 text-purple-800";
            case WALKIN -> "bg-orange-100 text-orange-800";
        };
        this.canReceive = r.getStatus() == ReservationStatus.RESERVED
                && r.getSource() != ReservationSource.WALKIN;
        this.canCancel = r.getStatus() == ReservationStatus.RESERVED
                || r.getStatus() == ReservationStatus.RECEIVED;

    }
}
