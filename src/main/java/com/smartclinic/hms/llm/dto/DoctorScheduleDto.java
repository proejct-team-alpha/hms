package com.smartclinic.hms.llm.dto;

import com.smartclinic.hms.domain.DoctorSchedule;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalTime;

@Getter
@AllArgsConstructor
public class DoctorScheduleDto {

    private String dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private Boolean isAvailable;

    public static DoctorScheduleDto from(DoctorSchedule entity) {
        return new DoctorScheduleDto(
                entity.getDayOfWeek(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getIsAvailable()
        );
    }
}
