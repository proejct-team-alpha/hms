package com.smartclinic.hms.doctor.treatment.dto;

import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationStatus;
import com.smartclinic.hms.domain.TreatmentRecord;
import lombok.Getter;

@Getter
public class DoctorTreatmentDetailDto {

    private final Long reservationId;
    private final String patientName;
    private final String patientPhone;
    private final String note;
    private final String timeSlot;
    private final String statusText;
    private final boolean canComplete;
    private final String diagnosis;
    private final String prescription;
    private final String remark;

    public DoctorTreatmentDetailDto(Reservation r, TreatmentRecord record) {
        this.reservationId = r.getId();
        this.patientName = r.getPatient().getName();
        this.patientPhone = r.getPatient().getPhone();
        this.note = r.getPatient().getNote() != null ? r.getPatient().getNote() : "증상 없음";
        this.timeSlot = r.getTimeSlot();
        this.canComplete = r.getStatus() == ReservationStatus.RECEIVED;
        this.statusText = switch (r.getStatus()) {
            case RECEIVED -> "진료 대기";
            case COMPLETED -> "진료 완료";
            default -> "예약";
        };

        if (record != null) {
            this.diagnosis = record.getDiagnosis();
            this.prescription = record.getPrescription();
            this.remark = record.getRemark();
        } else {
            this.diagnosis = null;
            this.prescription = null;
            this.remark = null;
        }
    }
}
