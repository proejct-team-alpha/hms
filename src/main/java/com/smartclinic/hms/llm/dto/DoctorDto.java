package com.smartclinic.hms.llm.dto;

import com.smartclinic.hms.domain.Doctor;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DoctorDto {

    private Long id;
    private String name;
    private String department;
    private String specialty;
    private String phone;
    private String email;

    public static DoctorDto from(Doctor doctor) {
        return new DoctorDto(
                doctor.getId(),
                doctor.getStaff().getName(),
                doctor.getDepartment().getName(),
                doctor.getSpecialty(),
                doctor.getStaff().getPhone(),
                doctor.getStaff().getEmail()
        );
    }
}
