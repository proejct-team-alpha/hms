package com.smartclinic.hms.staff.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StaffHourlyStatDto {
    private String label; // "09:00", "10:00" 등
    private int reservationCount; // 예약 대기
    private int receptionCount;   // 접수 완료
    private int totalCount;       // 전체 (예약 + 접수)
}
