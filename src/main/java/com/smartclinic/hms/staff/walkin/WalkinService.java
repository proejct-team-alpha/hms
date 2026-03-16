package com.smartclinic.hms.staff.walkin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartclinic.hms.common.exception.CustomException;
import com.smartclinic.hms.common.util.ReservationNumberGenerator;
import com.smartclinic.hms.doctor.DoctorRepository;
import com.smartclinic.hms.domain.Department;
import com.smartclinic.hms.domain.Doctor;
import com.smartclinic.hms.domain.Patient;
import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationSource;
import com.smartclinic.hms.reservation.reservation.DepartmentRepository;
import com.smartclinic.hms.reservation.reservation.PatientRepository;
import com.smartclinic.hms.reservation.reservation.ReservationRepository;
import com.smartclinic.hms.staff.walkin.dto.WalkinRequestDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalkinService {

        private final ReservationRepository reservationRepository;
        private final PatientRepository patientRepository;
        private final DoctorRepository doctorRepository;
        private final DepartmentRepository departmentRepository;
        private final ReservationNumberGenerator reservationNumberGenerator;

        // 방문 접수 생성
        @Transactional
        public void createWalkin(WalkinRequestDto request) {

                // 1. 기존 환자 조회 (없으면 신규 생성)
                Patient patient = patientRepository.findByPhone(request.getPhone())
                                .orElseGet(() -> patientRepository.save(
                                                Patient.create(
                                                                request.getName(),
                                                                request.getPhone(),
                                                                null)));

                Doctor doctor = doctorRepository.findById(request.getDoctorId())
                                .orElseThrow(() -> CustomException
                                                .notFound("의사를 찾을 수 없습니다. ID: " + request.getDoctorId()));

                Department department = departmentRepository.findById(request.getDepartmentId())
                                .orElseThrow(() -> CustomException
                                                .notFound("진료과를 찾을 수 없습니다. ID: " + request.getDepartmentId()));

                String reservationNumber = reservationNumberGenerator.generate(
                                request.getDate(),
                                () -> reservationRepository.countByReservationDate(request.getDate()));

                Reservation reservation = Reservation.create(
                                reservationNumber,
                                patient,
                                doctor,
                                department,
                                request.getDate(),
                                request.getTime(),
                                ReservationSource.WALKIN);

                reservationRepository.save(reservation);
                reservation.receive(); // [PRD F03-4] 워크인 즉시 접수: RESERVED → RECEIVED
        }
}
