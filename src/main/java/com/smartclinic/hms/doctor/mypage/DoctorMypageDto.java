package com.smartclinic.hms.doctor.mypage;

import com.smartclinic.hms.domain.Doctor;
import lombok.Getter;

@Getter
public class DoctorMypageDto {

    private final String name;
    private final String username;
    private final String employeeNumber;
    private final String department;
    private final String specialty;
    private final String availableDays;
    private final String email;
    private final String phone;

    public DoctorMypageDto(Doctor doctor) {
        this.name = doctor.getStaff().getName();
        this.username = doctor.getStaff().getUsername();
        this.employeeNumber = doctor.getStaff().getEmployeeNumber();
        this.department = doctor.getDepartment() != null ? doctor.getDepartment().getName() : "-";
        this.specialty = doctor.getSpecialty() != null ? doctor.getSpecialty() : "-";
        this.availableDays = formatAvailableDays(doctor.getAvailableDays());
        this.email = doctor.getStaff().getEmail() != null ? doctor.getStaff().getEmail() : "";
        this.phone = doctor.getStaff().getPhone() != null ? doctor.getStaff().getPhone() : "";
    }

    private String formatAvailableDays(String days) {
        if (days == null || days.isBlank()) return "월~금";
        return days.replace("MON", "월").replace("TUE", "화").replace("WED", "수")
                   .replace("THU", "목").replace("FRI", "금").replace("SAT", "토")
                   .replace("SUN", "일").replace(",", ", ");
    }
}
