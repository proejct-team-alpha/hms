package com.smartclinic.hms.nurse.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 간호사 대시보드 전체 데이터를 담는 DTO
 */
@Getter
@RequiredArgsConstructor
public class NurseDashboardDto {
    private final List<NurseDoctorStatusDto> doctorStatuses; // 모든 의사의 현황 목록
}
