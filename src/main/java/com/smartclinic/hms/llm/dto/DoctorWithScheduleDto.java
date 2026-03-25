package com.smartclinic.hms.llm.dto;

import com.smartclinic.hms.domain.Doctor;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class DoctorWithScheduleDto {

    private Long id;
    private String name;
    private String department;
    private String specialty;
    private String phone;
    private String email;
    private List<DoctorScheduleDto> schedules;

    public static DoctorWithScheduleDto from(Doctor doctor, List<DoctorScheduleDto> schedules) {
        return new DoctorWithScheduleDto(
                doctor.getId(),
                doctor.getStaff().getName(),
                doctor.getDepartment().getName(),
                doctor.getSpecialty(),
                doctor.getStaff().getPhone(),
                doctor.getStaff().getEmail(),
                schedules
        );
    }
}
