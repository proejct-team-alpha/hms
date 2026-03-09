package com.smartclinic.hms.reservation.reservation;

import com.smartclinic.hms.doctor.DoctorDto;
import com.smartclinic.hms.doctor.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final DoctorRepository doctorRepository;

    @Transactional(readOnly = true)
    public List<DoctorDto> getDoctorsByDepartment(Long departmentId) {
        return doctorRepository.findByDepartment_Id(departmentId)
                .stream()
                .map(DoctorDto::new)
                .toList();
    }
}
