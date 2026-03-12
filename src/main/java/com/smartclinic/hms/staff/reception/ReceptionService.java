package com.smartclinic.hms.staff.reception;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.smartclinic.hms.staff.reservation.dto.PhoneReservationRequestDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReceptionService {

    private final ReservationRepository reservationRepository;
    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final PatientRepository patientRepository;
    private final ReservationNumberGenerator reservationNumberGenerator;

    @Transactional
    public void createPhoneReservation(PhoneReservationRequestDto request) {

        String phone = request.getPhone().trim();

        // 1️ 환자 조회
        Patient patient = patientRepository.findByPhone(phone).orElse(null);

        // 2️ 없으면 신규 환자 생성
        if (patient == null) {
            patient = Patient.create(
                    request.getName(),
                    phone,
                    request.getEmail());
            patientRepository.save(patient);
        }

        // 3️ 의사 조회
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new RuntimeException("의사 없음"));

        // 4️ 진료과 조회
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("진료과 없음"));

        // 5️ 예약 날짜
        LocalDate reservationDate = LocalDate.parse(request.getDate());

        // 6️ 예약번호 생성
        String reservationNumber = reservationNumberGenerator.generate(
                reservationDate,
                () -> reservationRepository.countByReservationDate(reservationDate));

        // 7️ 예약 생성
        Reservation reservation = Reservation.create(
                reservationNumber,
                patient,
                doctor,
                department,
                reservationDate,
                request.getTime(),
                ReservationSource.PHONE);

        reservationRepository.save(reservation);
    }
}