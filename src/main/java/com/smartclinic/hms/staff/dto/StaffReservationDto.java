package com.smartclinic.hms.staff.dto;

import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationStatus;
import com.smartclinic.hms.domain.ReservationSource;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StaffReservationDto {

    private final Long id;
    private final Long patientId; 
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
    private String statusText;
    private String statusBadgeClass;
    private final String sourceText;
    private final String sourceBadgeClass;
    private final String ageGender; 
    private final String birthInfo; 
    private final String birthDatePart; 
    private final String genderDigitPart; 
    private final String visitReason; 
    private final String cancellationReason;
    private final String actualReceptionDate; 
    private final String receptionTime; 
    private final boolean treatmentCompleted; 
    private final boolean paid; 
    private final boolean canReceive;
    private final boolean canCancel;
    /* [기능 구현] 최종 진료과, 담당 의사, 내원 사유의 수정 가능 여부 판단 플래그 */
    private final boolean canEditReceptionInfo;
    private final boolean isFirstVisit;
    private final long visitCount;
    private final java.util.List<com.smartclinic.hms.domain.PatientHistoryDto> history;

    public StaffReservationDto(Reservation r) {
        this(r, 0L, new java.util.ArrayList<>()); 
    }

    public StaffReservationDto(Reservation r, long completedCount) {
        this(r, completedCount, new java.util.ArrayList<>()); 
    }

    public StaffReservationDto(Reservation r, long completedCount, java.util.List<com.smartclinic.hms.domain.PatientHistoryDto> history) {
        this.id = r.getId();
        this.patientId = r.getPatient().getId(); 
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
        this.cancellationReason = r.getCancellationReason() != null ? r.getCancellationReason() : "-";
        this.history = history;

        String bInfo = r.getPatient().getBirthInfo();
        String ageGen = "-";
        if (bInfo != null && bInfo.contains("-") && bInfo.length() >= 8) {
            try {
                String[] parts = bInfo.split("-");
                int birthYear2Digit = Integer.parseInt(parts[0].substring(0, 2));
                String genderDigit = parts[1];
                int currentYear = java.time.LocalDate.now().getYear();
                int fullBirthYear = (genderDigit.equals("1") || genderDigit.equals("2")) ? 1900 + birthYear2Digit : 2000 + birthYear2Digit;
                int age = currentYear - fullBirthYear + 1;
                String gender = (genderDigit.equals("1") || genderDigit.equals("3")) ? "M" : "F";
                ageGen = gender + " / " + age + " 세";
            } catch (Exception e) { ageGen = "-"; }
        }
        this.ageGender = ageGen;
        this.birthInfo = bInfo != null ? bInfo : "";
        
        if (this.birthInfo != null && this.birthInfo.contains("-")) {
            String[] parts = this.birthInfo.split("-");
            this.birthDatePart = (parts.length > 0) ? parts[0] : "";
            this.genderDigitPart = (parts.length > 1) ? parts[1] : "";
        } else {
            this.birthDatePart = (this.birthInfo != null) ? this.birthInfo : "";
            this.genderDigitPart = "";
        }

        this.visitReason = r.getPatient().getVisitReason() != null ? r.getPatient().getVisitReason() : "";
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
        this.canCancel = r.getStatus() == ReservationStatus.RESERVED || r.getStatus() == ReservationStatus.RECEIVED;
        this.treatmentCompleted = r.isTreatmentCompleted();
        this.paid = r.isPaid();
        this.canEditReceptionInfo = !this.paid && !this.treatmentCompleted && 
                                   (r.getStatus() == ReservationStatus.RESERVED || r.getStatus() == ReservationStatus.RECEIVED);
        this.visitCount = completedCount;
        this.isFirstVisit = (completedCount == 0);

        if (r.getReceptionTime() != null) {
            this.actualReceptionDate = r.getReceptionTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            this.receptionTime = r.getReceptionTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        } else {
            this.actualReceptionDate = null;
            this.receptionTime = null;
        }

        // [핵심수정] 상태 텍스트 및 배지 클래스를 '단 한 번'만 할당하도록 통합
        if (this.paid) {
            this.statusText = "수납 완료";
            this.statusBadgeClass = "bg-green-100 text-green-800";
        } else if (this.treatmentCompleted) {
            this.statusText = "처치 완료";
            this.statusBadgeClass = "bg-purple-100 text-purple-700";
        } else {
            this.statusText = switch (r.getStatus()) {
                case RESERVED -> "예약";
                case RECEIVED -> "진료 대기";
                case IN_TREATMENT -> "진료중";
                case COMPLETED -> "진료 완료";
                case CANCELLED -> "취소";
            };
            this.statusBadgeClass = switch (r.getStatus()) {
                case RESERVED -> "bg-indigo-100 text-indigo-700";
                case RECEIVED -> "bg-orange-100 text-orange-700";
                case IN_TREATMENT -> "bg-indigo-100 text-indigo-800";
                case COMPLETED -> "bg-green-100 text-green-700";
                case CANCELLED -> "bg-slate-100 text-slate-500";
            };
        }
    }

    public String getActualReceptionDate() { return actualReceptionDate; }
    public String getReceptionTime() { return receptionTime; }
    public boolean isTreatmentCompleted() { return treatmentCompleted; }
    public boolean isPaid() { return paid; }
}
