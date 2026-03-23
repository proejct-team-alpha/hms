package com.smartclinic.hms.doctor.treatment.dto;

import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationStatus;
import lombok.Getter;

@Getter
public class DoctorReservationDto {

    private final Long id;
    private final String patientName;
    private final String patientPhone;
    private final String timeSlot;
    private final String note;
    private final String statusText;
    private final String statusBadgeClass;
    private final String cardClass;
    private final String sourceText;
    private final String sourceBadgeClass;
    private final boolean canComplete;
    private final boolean canStartTreatment;

    /**
     * 초진 여부 (true: 초진, false: 재진)
     */
    private final boolean isFirstVisit;

    /**
     * 확정된 진단명 (진료 완료 목록용)
     */
    private final String diagnosis;

    public DoctorReservationDto(Reservation r) {
        this(r, false, null); // 기본값 설정
    }

    public DoctorReservationDto(Reservation r, boolean isFirstVisit) {
        this(r, isFirstVisit, null);
    }

    public DoctorReservationDto(Reservation r, boolean isFirstVisit, String diagnosis) {
        this.id = r.getId();
        this.patientName = r.getPatient().getName();
        this.patientPhone = r.getPatient().getPhone();
        this.timeSlot = r.getTimeSlot();
        this.note = r.getPatient().getNote() != null ? r.getPatient().getNote() : "증상 없음";
        this.isFirstVisit = isFirstVisit;
        this.diagnosis = diagnosis;

        switch (r.getStatus()) {
            case RECEIVED -> {
                this.statusText = "진료 대기";
                this.statusBadgeClass = "bg-orange-100 text-orange-600";
                this.cardClass = "border-slate-200 hover:border-indigo-300";
                this.canComplete = true;
                this.canStartTreatment = true;
            }
            case IN_TREATMENT -> {
                this.statusText = "진료중";
                this.statusBadgeClass = "bg-indigo-100 text-indigo-700";
                this.cardClass = "border-indigo-300 hover:border-indigo-400";
                this.canComplete = true;
                this.canStartTreatment = false;
            }
            case COMPLETED -> {
                this.statusText = "진료 완료";
                this.statusBadgeClass = "bg-green-600 text-white";
                this.cardClass = "border-slate-200 opacity-60";
                this.canComplete = false;
                this.canStartTreatment = false;
            }
            default -> {
                this.statusText = "예약";
                this.statusBadgeClass = "bg-slate-100 text-slate-600";
                this.cardClass = "border-slate-200 hover:border-indigo-300";
                this.canComplete = false;
                this.canStartTreatment = false;
            }
        }

        switch (r.getSource()) {
            case ONLINE -> {
                this.sourceText = "온라인";
                this.sourceBadgeClass = "bg-blue-50 text-blue-600";
            }
            case PHONE -> {
                this.sourceText = "전화";
                this.sourceBadgeClass = "bg-purple-50 text-purple-600";
            }
            default -> {
                this.sourceText = "방문";
                this.sourceBadgeClass = "bg-orange-50 text-orange-600";
            }
        }
    }
}
