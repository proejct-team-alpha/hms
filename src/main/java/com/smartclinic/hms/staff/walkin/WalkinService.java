package com.smartclinic.hms.staff.walkin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartclinic.hms.domain.Department;
import com.smartclinic.hms.domain.Doctor;
import com.smartclinic.hms.domain.Patient;
import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationSource;
import com.smartclinic.hms.common.util.ReservationNumberGenerator;
import com.smartclinic.hms.doctor.DoctorRepository;
import com.smartclinic.hms.reservation.reservation.PatientRepository;
import com.smartclinic.hms.reservation.reservation.DepartmentRepository;
import com.smartclinic.hms.reservation.reservation.ReservationRepository;
import com.smartclinic.hms.staff.walkin.dto.WalkinRequestDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class WalkinService {

        private final ReservationRepository reservationRepository;
        private final PatientRepository patientRepository;
        private final DoctorRepository doctorRepository;
        private final DepartmentRepository departmentRepository;
        private final ReservationNumberGenerator reservationNumberGenerator;

        // 방문 접수 생성

        public void createWalkin(WalkinRequestDto request) {

                Patient patient = patientRepository.findByPhone(request.getPhone())
                                .orElseGet(() -> patientRepository.save(
                                                Patient.create(
                                                                request.getName(),
                                                                request.getPhone(),
                                                                null)));

                Doctor doctor = doctorRepository.findById(request.getDoctorId())
                                .orElseThrow();

                Department department = departmentRepository.findById(request.getDepartmentId())
                                .orElseThrow();

                Reservation reservation = Reservation.create(
                                "WALKIN-" + System.currentTimeMillis(),
                                patient,
                                doctor,
                                department,
                                request.getDate(),
                                request.getTime(),
                                ReservationSource.WALKIN);

                reservationRepository.save(reservation);
        }
}