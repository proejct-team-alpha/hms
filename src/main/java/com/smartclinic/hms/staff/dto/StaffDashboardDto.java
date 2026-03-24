package com.smartclinic.hms.staff.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class StaffDashboardDto {

    private final int totalToday;
    private final int waitingCount;
    private final int receivedCount;
    /* [기능 구현] 오늘 수납 완료 건수 */
    private final int paidCount;
    private final List<StaffReservationDto> recentList;
    private final List<StaffHourlyStatDto> hourlyStats;
    private final boolean hasRecent;

    public StaffDashboardDto(int totalToday, int waitingCount, int receivedCount, int paidCount,
                             List<StaffReservationDto> recentList,
                             List<StaffHourlyStatDto> hourlyStats) {
        this.totalToday   = totalToday;
        this.waitingCount = waitingCount;
        this.receivedCount = receivedCount;
        this.paidCount     = paidCount;
        this.recentList   = recentList;
        this.hourlyStats  = hourlyStats;
        this.hasRecent    = !recentList.isEmpty();
    }
}
