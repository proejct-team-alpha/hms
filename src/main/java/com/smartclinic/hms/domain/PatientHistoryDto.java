package com.smartclinic.hms.domain;

import lombok.Getter;
import java.time.format.DateTimeFormatter;

/**
 * 환자의 과거 통합 진료 이력을 담는 DTO
 * (진료가 완료된 기록만 표시하는 용도)
 */
@Getter
public class PatientHistoryDto {
    private final String date;          // 진료 일자
    private final String deptName;      // 진료과
    private final String doctorName;    // 담당 의사
    private final String diagnosis;     // 진단명
    private final String remark;        // 진료 소견 (SOAP)
    private final String prescription;  // 처방 내역
    private final String statusText;    // 예약 상태 텍스트
    private final String statusBadgeClass; // 상태별 UI 색상

    public PatientHistoryDto(Reservation r, TreatmentRecord record) {
        this.date = r.getReservationDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        this.deptName = r.getDepartment().getName();
        this.doctorName = r.getDoctor().getStaff().getName();
        
        // 상태별 텍스트 및 배지 색상 설정
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
            case CANCELLED -> "bg-red-100 text-red-700";
        };
        
        // 진료 기록이 있는 경우에만 데이터를 채우고, 없으면 "-"로 표시
        if (record != null) {
            this.diagnosis = record.getDiagnosis();
            this.remark = record.getRemark() != null ? record.getRemark() : "";
            this.prescription = record.getPrescription();
        } else {
            this.diagnosis = "기록 없음";
            this.remark = "";
            this.prescription = "-";
        }
    }
}
