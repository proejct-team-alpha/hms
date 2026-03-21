package com.smartclinic.hms.nurse.dto;

import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationSource;
import com.smartclinic.hms.domain.ReservationStatus;
import lombok.Getter;

@Getter
public class NursePatientStatusDto {

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

    /**
     * 처치 완료 여부
     */
    private final boolean treatmentCompleted;

    /**
     * 수납 완료 여부
     */
    private final boolean isPaid;

    /**
     * 처치 완료 처리가 가능한 상태인지 (진료 완료 후 아직 처치 미완료)
     */
    private final boolean canCompleteTreatment;

    /**
     * 초진 여부 (true: 초진, false: 재진)
     */
    private final boolean isFirstVisit;

    public NursePatientStatusDto(Reservation r) {
        this(r, false);
    }

    public NursePatientStatusDto(Reservation r, boolean isFirstVisit) {
        this.id = r.getId();
        this.reservationNumber = r.getReservationNumber();
        this.patientId = r.getPatient().getId();
        this.patientName = r.getPatient().getName();
        this.patientPhone = r.getPatient().getPhone();
        this.timeSlot = r.getTimeSlot();
        this.doctorName = r.getDoctor().getStaff().getName();
        this.departmentName = r.getDepartment().getName();
        this.treatmentCompleted = r.isTreatmentCompleted();
        this.isPaid = r.isPaid();
        this.statusText = toStatusText(r);
        this.statusBadgeClass = toStatusBadgeClass(r);
        this.sourceText = toSourceText(r.getSource());
        this.canReceive = r.getStatus() == ReservationStatus.RESERVED && !r.isPaid();
        this.canCompleteTreatment = r.getStatus() == ReservationStatus.COMPLETED && !r.isTreatmentCompleted() && !r.isPaid();
        this.isFirstVisit = isFirstVisit;
    }

    private static String toStatusText(Reservation r) {
        if (r.isPaid()) return "수납 완료";
        if (r.isTreatmentCompleted()) return "처치 완료";
        
        return switch (r.getStatus()) {
            case RESERVED -> "예약됨";
            case RECEIVED -> "진료 대기";
            case IN_TREATMENT -> "진료중";
            case COMPLETED -> "진료 완료";
            case CANCELLED -> "취소됨";
        };
    }

    private static String toStatusBadgeClass(Reservation r) {
        if (r.isPaid()) return "bg-purple-100 text-purple-800";
        if (r.isTreatmentCompleted()) return "bg-blue-100 text-blue-800";

        return switch (r.getStatus()) {
            case RESERVED -> "bg-slate-100 text-slate-600";
            case RECEIVED -> "bg-yellow-100 text-yellow-800";
            case IN_TREATMENT -> "bg-indigo-100 text-indigo-800";
            case COMPLETED -> "bg-green-100 text-green-800";
            case CANCELLED -> "bg-red-100 text-red-600";
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
