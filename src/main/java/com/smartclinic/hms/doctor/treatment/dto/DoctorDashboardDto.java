package com.smartclinic.hms.doctor.treatment.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class DoctorDashboardDto {
    private final int totalToday;      // 오늘의 환자 수 (대기 + 완료)
    private final int waitingCount;    // 대기 환자 수 (RECEIVED + IN_TREATMENT)
    private final int completedCount;  // 진료 완료 수 (COMPLETED)
    private final int reservedCount;   // 예약 환자 수 (RESERVED)
    private final String boxClass;     // 배경 및 테두리 스타일
    private final String iconClass;    // 아이콘 배경 및 색상 스타일
    private final String iconName;     // feather 아이콘 이름
    private final String btnClass;     // 버튼 스타일
    private final List<DoctorReservationDto> schedulePreview;
}
