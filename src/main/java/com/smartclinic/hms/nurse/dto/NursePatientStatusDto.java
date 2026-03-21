package com.smartclinic.hms.nurse.dto;

import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationSource;
import com.smartclinic.hms.domain.ReservationStatus;
import lombok.Getter;

import java.time.LocalDate;
import java.time.Period;

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
     * 성별 / 나이 (예: 남 / 28)
     */
    private final String genderAge;

    /**
     * 목록에서의 순번
     */
    private int sequence;

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
        
        // 성별/나이 파싱 (birthInfo: "940101-2" 형식 기반)
        String birthInfo = r.getPatient().getBirthInfo();
        String parsedGenderAge = "-";
        
        if (birthInfo != null && birthInfo.contains("-")) {
            try {
                String[] parts = birthInfo.split("-");
                String birthPart = parts[0];
                String genderPart = parts[1];

                String gender = "미상";
                if ("1".equals(genderPart) || "3".equals(genderPart)) gender = "남";
                else if ("2".equals(genderPart) || "4".equals(genderPart)) gender = "여";

                int birthYear = Integer.parseInt(birthPart.substring(0, 2));
                int birthMonth = Integer.parseInt(birthPart.substring(2, 4));
                int birthDay = Integer.parseInt(birthPart.substring(4, 6));

                if ("1".equals(genderPart) || "2".equals(genderPart)) birthYear += 1900;
                else birthYear += 2000;

                LocalDate birthDate = LocalDate.of(birthYear, birthMonth, birthDay);
                int age = Period.between(birthDate, LocalDate.now()).getYears();
                
                parsedGenderAge = gender + " / " + age;
            } catch (Exception e) {
                // 파싱 에러 시 기본값 "-" 유지
            }
        }
        this.genderAge = parsedGenderAge;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
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
