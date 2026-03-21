package com.smartclinic.hms.nurse.dto;

import com.smartclinic.hms.domain.Patient;
import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
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
    private final boolean isTreatmentCompleted;
    
    // 의사 화면과 동일한 필드 추가
    private final String genderAge;
    private final String visitReason;
    private final String sourceText;
    private final String sourceBadgeClass;
    private final String reservationDate;

    // 진료 및 처치 기록 (추가)
    private String diagnosis;
    private String prescription;
    private String remark;
    private String nurseNote;

    // 과거 진료 내역 (추가)
    private List<NurseTreatmentHistoryDto> history;

    /**
     * 초진 여부 (true: 초진, false: 재진)
     */
    private final boolean isFirstVisit;

    public NursePatientDto(Reservation r) {
        this(r, false);
    }

    public NursePatientDto(Reservation r, boolean isFirstVisit) {
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
                       || r.getStatus() == ReservationStatus.IN_TREATMENT
                       || r.getStatus() == ReservationStatus.COMPLETED; 
        this.isTreatmentCompleted = r.isTreatmentCompleted();
        this.isFirstVisit = isFirstVisit;
        
        // 의사 화면용 데이터 추가
        this.reservationDate = r.getReservationDate().toString();
        this.visitReason = p.getVisitReason() != null ? p.getVisitReason() : "-";
        this.genderAge = p.getBirthInfo() != null ? p.getBirthInfo() : "-"; // 보통 생년월일이나 성별/나이 정보를 담음
        this.sourceText = toSourceText(r.getSource());
        this.sourceBadgeClass = toSourceBadgeClass(r.getSource());
    }

    private static String toSourceText(com.smartclinic.hms.domain.ReservationSource s) {
        return switch (s) {
            case ONLINE -> "온라인 예약";
            case PHONE -> "전화 예약";
            case WALKIN -> "현장 방문";
        };
    }

    private static String toSourceBadgeClass(com.smartclinic.hms.domain.ReservationSource s) {
        return switch (s) {
            case ONLINE -> "bg-blue-50 text-blue-700 border-blue-100";
            case PHONE -> "bg-green-50 text-green-700 border-green-100";
            case WALKIN -> "bg-purple-50 text-purple-700 border-purple-100";
        };
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

    @Getter
    @Setter
    public static class NurseTreatmentHistoryDto {
        private String date;
        private Long deptId;      // 필터용 추가
        private Long doctorId;    // 필터용 추가
        private String doctorName;
        private String deptName;
        private String diagnosis;
        private String prescription;
        private String remark;     // 히스토리에 소견 추가
        private String nurseNote;

        public NurseTreatmentHistoryDto(String date, Long deptId, Long doctorId, String doctorName, String deptName, String diagnosis, String prescription, String remark, String nurseNote) {
            this.date = date;
            this.deptId = deptId;
            this.doctorId = doctorId;
            this.doctorName = doctorName;
            this.deptName = deptName;
            this.diagnosis = diagnosis;
            this.prescription = prescription;
            this.remark = remark;
            this.nurseNote = nurseNote;
        }
    }
}
