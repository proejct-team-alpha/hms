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
    private final String reservationDate;
    private final String statusText;
    private final boolean canComplete;
    private final boolean canStartTreatment;
    private final String diagnosis;
    private final String prescription;
    private final String remark;
    private final String sourceText;
    private final String sourceBadgeClass;

    /**
     * 초진 여부 (true: 초진, false: 재진)
     */
    private final boolean isFirstVisit;

    /**
     * 과거 진료 이력 목록 (최신순)
     */
    private final java.util.List<com.smartclinic.hms.domain.PatientHistoryDto> history;

    public DoctorTreatmentDetailDto(Reservation r, TreatmentRecord record) {
        this(r, record, false, new java.util.ArrayList<>());
    }

    public DoctorTreatmentDetailDto(Reservation r, TreatmentRecord record, boolean isFirstVisit, java.util.List<com.smartclinic.hms.domain.PatientHistoryDto> history) {
        this.reservationId = r.getId();
        this.patientName = r.getPatient().getName();
        this.patientPhone = r.getPatient().getPhone();
        this.note = r.getPatient().getNote() != null ? r.getPatient().getNote() : "증상 없음";
        this.timeSlot = r.getTimeSlot();
        this.reservationDate = r.getReservationDate().toString();
        this.isFirstVisit = isFirstVisit;
        this.history = history;
        this.canStartTreatment = r.getStatus() == ReservationStatus.RECEIVED;
        this.canComplete = r.getStatus() == ReservationStatus.RECEIVED
                        || r.getStatus() == ReservationStatus.IN_TREATMENT;
        this.statusText = switch (r.getStatus()) {
            case RECEIVED -> "진료 대기";
            case IN_TREATMENT -> "진료중";
            case COMPLETED -> "진료 완료";
            default -> "예약";
        };

        this.sourceText = switch (r.getSource()) {
            case ONLINE -> "온라인";
            case PHONE -> "전화";
            default -> "방문";
        };
        this.sourceBadgeClass = switch (r.getSource()) {
            case ONLINE -> "bg-blue-50 text-blue-600";
            case PHONE -> "bg-purple-50 text-purple-600";
            default -> "bg-orange-50 text-orange-600";
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
