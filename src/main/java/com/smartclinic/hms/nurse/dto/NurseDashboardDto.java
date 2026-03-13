package com.smartclinic.hms.nurse.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class NurseDashboardDto {
    private final long totalToday;
    private final long waitingCount;
    private final long specialCount;
    private final List<NurseReservationDto> waitingList;
}
