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
    private final String visitReason;
    private final String genderAge;
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

    /**
     * 필터링용 진료과 목록 (과거 이력 기반)
     */
    private final java.util.List<com.smartclinic.hms.staff.dto.StaffDepartmentOptionDto> filterDepartments;

    /**
     * 필터링용 의사 목록 (과거 이력 기반)
     */
    private final java.util.List<com.smartclinic.hms.staff.dto.StaffDoctorOptionDto> filterDoctors;

    public DoctorTreatmentDetailDto(Reservation r, TreatmentRecord record) {
        this(r, record, false, new java.util.ArrayList<>(), new java.util.ArrayList<>(), new java.util.ArrayList<>());
    }

    public DoctorTreatmentDetailDto(Reservation r, TreatmentRecord record, boolean isFirstVisit, 
                                   java.util.List<com.smartclinic.hms.domain.PatientHistoryDto> history,
                                   java.util.List<com.smartclinic.hms.staff.dto.StaffDepartmentOptionDto> filterDepartments,
                                   java.util.List<com.smartclinic.hms.staff.dto.StaffDoctorOptionDto> filterDoctors) {
        this.reservationId = r.getId();
        this.patientName = r.getPatient().getName();
        this.patientPhone = r.getPatient().getPhone();
        this.visitReason = (r.getPatient().getVisitReason() != null && !r.getPatient().getVisitReason().isBlank()) 
                ? r.getPatient().getVisitReason() : "-";
        this.timeSlot = r.getTimeSlot();
        this.reservationDate = r.getReservationDate().toString();
        this.isFirstVisit = isFirstVisit;
        this.history = history;
        this.filterDepartments = filterDepartments;
        this.filterDoctors = filterDoctors;

        // 주민번호 기반 성별 및 만 나이 계산
        String rn = r.getPatient().getResidentNumber();
        String gender = null;
        Integer age = null;
        if (rn != null && rn.contains("-")) {
            try {
                String[] parts = rn.split("-");
                String birthPart = parts[0];
                String genderPart = parts[1];
                if ("1".equals(genderPart) || "3".equals(genderPart)) gender = "M";
                else if ("2".equals(genderPart) || "4".equals(genderPart)) gender = "F";
                int birthYear = Integer.parseInt(birthPart.substring(0, 2));
                int birthMonth = Integer.parseInt(birthPart.substring(2, 4));
                int birthDay = Integer.parseInt(birthPart.substring(4, 6));
                if ("1".equals(genderPart) || "2".equals(genderPart)) birthYear += 1900;
                else birthYear += 2000;
                java.time.LocalDate birthDate = java.time.LocalDate.of(birthYear, birthMonth, birthDay);
                age = java.time.Period.between(birthDate, java.time.LocalDate.now()).getYears();
            } catch (Exception e) {
                // 에러 시 null 유지
            }
        }
        
        if (gender != null && age != null) {
            this.genderAge = gender + " / " + age + "세";
        } else {
            this.genderAge = "-";
        }

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
