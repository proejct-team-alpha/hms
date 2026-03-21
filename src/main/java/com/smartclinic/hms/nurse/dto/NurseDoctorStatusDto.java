package com.smartclinic.hms.nurse.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 간호사 대시보드 - 의사별 실시간 진료 현황 정보를 담는 DTO
 */
@Getter
@RequiredArgsConstructor
public class NurseDoctorStatusDto {
    private final String doctorName;      // 의사 이름
    private final String departmentName;  // 진료과 이름
    private final boolean isAvailableToday; // 오늘 진료 요일 여부
    
    private final long totalToday;        // 오늘 총 예약 건수
    private final long waitingCount;      // 진료 대기 (RECEIVED)
    private final long treatingCount;     // 진료 중 (IN_TREATMENT)
    private final long pendingTreatmentCount; // 처치 대기 (COMPLETED && !treatmentCompleted)
    private final long paidCount;         // 수납 완료 (paid == true)

    /**
     * 오늘 진료가 없는 경우 시각적으로 구분하기 위한 스타일 클래스
     */
    public String getRowClass() {
        return isAvailableToday ? "" : "opacity-50 bg-slate-50 grayscale";
    }

    /**
     * 상태 텍스트
     */
    public String getAvailabilityText() {
        return isAvailableToday ? "진료 중" : "휴진";
    }
}
