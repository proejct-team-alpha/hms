package com.smartclinic.hms.staff.dto;

import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationStatus;
import com.smartclinic.hms.domain.ReservationSource;
import lombok.Getter;

@Getter
public class StaffReservationDto {

    private final Long id;
    private final Long patientId; // [신규 추가] 환자 고유 번호 (ID)
    private final String reservationNumber;
    private final String patientName;
    private final String patientPhone;
    private final String patientEmail;
    private final String patientAddress;
    private final String patientNote;
    private final String reservationDate;
    private final String timeSlot;
    private final Long departmentId;
    private final String departmentName;
    private final Long doctorId;
    private final String doctorName;
    private final String statusText;
    private final String statusBadgeClass;
    private final String sourceText;
    private final String sourceBadgeClass;
    private final String cancellationReason;
    private final boolean canReceive;
    private final boolean canCancel;

    // ==========================================
    // [신규 추가] 팀 프로젝트 - 환자 초재진 정보
    // ==========================================
    
    /**
     * 초진 여부 (true: 초진, false: 재진)
     * 진료 완료된 이력이 0건이면 초진으로 표시됩니다.
     */
    private final boolean isFirstVisit;

    /**
     * 총 진료 완료 횟수
     */
    private final long visitCount;

    /**
     * 과거 진료 이력 목록 (최신순)
     */
    private final java.util.List<com.smartclinic.hms.domain.PatientHistoryDto> history;

    public StaffReservationDto(Reservation r) {
        this(r, 0L, new java.util.ArrayList<>()); // 기본값 처리
    }

    public StaffReservationDto(Reservation r, long completedCount) {
        this(r, completedCount, new java.util.ArrayList<>()); // 히스토리 없이 생성하는 경우
    }

    /**
     * 초재진 정보를 포함한 상세 생성자
     * @param r 예약 엔티티
     * @param completedCount 해당 환자의 진료 완료된 예약 건수
     * @param history 과거 전체 히스토리 목록
     */
    public StaffReservationDto(Reservation r, long completedCount, java.util.List<com.smartclinic.hms.domain.PatientHistoryDto> history) {
        this.id = r.getId();
        this.patientId = r.getPatient().getId(); // [주석] 환자 엔티티의 고유 ID를 가져옵니다.
        this.reservationNumber = r.getReservationNumber();
        this.patientName = r.getPatient().getName();
        this.patientPhone = r.getPatient().getPhone();
        this.patientEmail = r.getPatient().getEmail() != null ? r.getPatient().getEmail() : "-";
        this.patientAddress = r.getPatient().getAddress() != null ? r.getPatient().getAddress() : "";
        this.patientNote = r.getPatient().getNote() != null ? r.getPatient().getNote() : "";
        this.reservationDate = r.getReservationDate().toString();
        this.timeSlot = r.getTimeSlot();
        this.departmentId = r.getDepartment().getId();
        this.departmentName = r.getDepartment().getName();
        this.doctorId = r.getDoctor().getId();
        this.doctorName = r.getDoctor().getStaff().getName();
        this.cancellationReason = r.getCancellationReason();
        this.history = history;

        this.statusText = switch (r.getStatus()) {
            case RESERVED -> "접수 대기";
            case RECEIVED -> "진료 대기";
            case IN_TREATMENT -> "진료중";
            case COMPLETED -> "진료 완료";
            case CANCELLED -> "취소됨";
        };
        this.statusBadgeClass = switch (r.getStatus()) {
            case RESERVED -> "bg-indigo-100 text-indigo-700";
            case RECEIVED -> "bg-orange-100 text-orange-700";
            case IN_TREATMENT -> "bg-indigo-100 text-indigo-800";
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
        this.canReceive = r.getStatus() == ReservationStatus.RESERVED;
        this.canCancel = r.getStatus() == ReservationStatus.RESERVED
                || r.getStatus() == ReservationStatus.RECEIVED;

        // 초재진 판별 로직 적용
        this.visitCount = completedCount;
        this.isFirstVisit = (completedCount == 0);
    }
}
