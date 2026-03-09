package com.smartclinic.hms.reservation.reservation;

import com.smartclinic.hms.doctor.DoctorDto;
import com.smartclinic.hms.doctor.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final DoctorRepository doctorRepository;

    public List<DoctorDto> getDoctorsByDepartment(Long departmentId) {
        return doctorRepository.findByDepartment_Id(departmentId)
                .stream()
                .map(DoctorDto::new)
                .toList();
    }
}
