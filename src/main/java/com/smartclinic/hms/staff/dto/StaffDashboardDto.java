package com.smartclinic.hms.staff.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class StaffDashboardDto {

    private final int totalToday;
    private final int waitingCount;
    private final int receivedCount;
    private final List<StaffReservationDto> recentList;
    private final boolean hasRecent;

    public StaffDashboardDto(int totalToday, int waitingCount, int receivedCount,
                             List<StaffReservationDto> recentList) {
        this.totalToday   = totalToday;
        this.waitingCount = waitingCount;
        this.receivedCount = receivedCount;
        this.recentList   = recentList;
        this.hasRecent    = !recentList.isEmpty();
    }
}
