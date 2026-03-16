package com.smartclinic.hms.doctor.treatment.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class DoctorDashboardDto {
    private final int totalPatients;
    private final int completedCount;
    private final int waitingCount;
    private final List<DoctorReservationDto> schedulePreview;
}
