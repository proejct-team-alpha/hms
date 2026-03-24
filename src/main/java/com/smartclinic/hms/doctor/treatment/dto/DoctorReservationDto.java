package com.smartclinic.hms.doctor.treatment.dto;

import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationStatus;
import lombok.Getter;

@Getter
public class DoctorReservationDto {

    private final Long id;
    private Integer sequence; // 리스트 순번
    private final String patientName;
    private final String patientPhone;
    private final String timeSlot;
    private final String receptionTime; // [기능 추가] 환자 접수 시간 필드
    private final String visitReason;
    private final String genderAge; // 예: "F / 30세"
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

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

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
        // [기능 추가] 접수 시간 포맷팅 (데이터가 없으면 "-" 처리)
        this.receptionTime = (r.getReceptionTime() != null) 
                ? r.getReceptionTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) 
                : "-";
        this.visitReason = (r.getPatient().getVisitReason() != null && !r.getPatient().getVisitReason().isBlank()) 
                ? r.getPatient().getVisitReason() : "-";
        this.isFirstVisit = isFirstVisit;
        this.diagnosis = diagnosis;

        // 환자의 생년월일 및 성별 정보(birthInfo)를 기반으로 성별 및 만 나이 계산
        String birthInfo = r.getPatient().getBirthInfo();
        String gender = null;
        Integer age = null;

        if (birthInfo != null && birthInfo.contains("-")) {
            try {
                String[] parts = birthInfo.split("-");
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
                // 파싱 에러 시 null 유지
            }
        }

        if (gender != null && age != null) {
            this.genderAge = gender + " / " + age + "세";
        } else {
            this.genderAge = "-";
        }

        switch (r.getStatus()) {
            case RECEIVED -> {
                this.statusText = "진료 대기";
                this.statusBadgeClass = "bg-blue-50 text-blue-600";
                this.cardClass = "border-slate-200 hover:border-indigo-300";
                this.canComplete = true;
                this.canStartTreatment = true;
            }
            case IN_TREATMENT -> {
                this.statusText = "진료중";
                this.statusBadgeClass = "bg-green-50 text-green-600";
                this.cardClass = "border-indigo-300 hover:border-indigo-400";
                this.canComplete = true;
                this.canStartTreatment = false;
            }
            case COMPLETED -> {
                this.statusText = "진료 완료";
                this.statusBadgeClass = "bg-slate-100 text-slate-500";
                this.cardClass = "border-slate-200 opacity-60";
                this.canComplete = false;
                this.canStartTreatment = false;
            }
            default -> {
                this.statusText = "예약";
                this.statusBadgeClass = "bg-purple-50 text-purple-600";
                this.cardClass = "border-purple-100 hover:border-purple-200";
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
