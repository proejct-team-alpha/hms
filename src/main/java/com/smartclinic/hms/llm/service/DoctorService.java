package com.smartclinic.hms.llm.service;

import com.smartclinic.hms.doctor.DoctorRepository;
import com.smartclinic.hms.domain.DoctorScheduleRepository;
import com.smartclinic.hms.llm.dto.DoctorDto;
import com.smartclinic.hms.llm.dto.DoctorScheduleDto;
import com.smartclinic.hms.llm.dto.DoctorWithScheduleDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;

    public List<DoctorDto> findDoctorsByDepartment(String department) {
        String normalized = normalizeDepartment(department);
        List<com.smartclinic.hms.domain.Doctor> doctors =
                doctorRepository.findByDepartmentNameAndActive(normalized);

        if (doctors.isEmpty()) {
            log.info("No doctors found for department: {}", normalized);
        }

        return doctors.stream()
                .map(DoctorDto::from)
                .toList();
    }

    public List<DoctorWithScheduleDto> findDoctorsWithSchedule(String department) {
        String normalized = normalizeDepartment(department);
        List<com.smartclinic.hms.domain.Doctor> doctors =
                doctorRepository.findByDepartmentNameAndActive(normalized);

        return doctors.stream()
                .map(doctor -> {
                    List<DoctorScheduleDto> schedules = doctorScheduleRepository
                            .findByDoctor_IdAndIsAvailableTrue(doctor.getId())
                            .stream()
                            .map(DoctorScheduleDto::from)
                            .toList();
                    return DoctorWithScheduleDto.from(doctor, schedules);
                })
                .toList();
    }

    private String normalizeDepartment(String department) {
        if (department == null) return "";
        return department.replaceAll("\\s+", "").trim();
    }
}
