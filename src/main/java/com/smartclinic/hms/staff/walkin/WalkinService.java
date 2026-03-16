package com.smartclinic.hms.staff.walkin;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

<<<<<<< HEAD
import com.smartclinic.hms.common.exception.CustomException;
import com.smartclinic.hms.common.util.ReservationNumberGenerator;
=======
import com.smartclinic.hms.common.util.ReservationNumberGenerator;
import com.smartclinic.hms.doctor.DoctorRepository;
>>>>>>> dev
import com.smartclinic.hms.domain.Department;
import com.smartclinic.hms.domain.Doctor;
import com.smartclinic.hms.domain.Patient;
import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationSource;
<<<<<<< HEAD
import com.smartclinic.hms.doctor.DoctorRepository;
=======
>>>>>>> dev
import com.smartclinic.hms.reservation.reservation.DepartmentRepository;
import com.smartclinic.hms.reservation.reservation.PatientRepository;
import com.smartclinic.hms.reservation.reservation.ReservationRepository;
import com.smartclinic.hms.staff.walkin.dto.WalkinRequestDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalkinService {

<<<<<<< HEAD
    private final ReservationRepository reservationRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final ReservationNumberGenerator reservationNumberGenerator;
=======
        private final ReservationRepository reservationRepository;
        private final PatientRepository patientRepository;
        private final DoctorRepository doctorRepository;
        private final DepartmentRepository departmentRepository;
        private final ReservationNumberGenerator reservationNumberGenerator;
>>>>>>> dev

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

<<<<<<< HEAD
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> CustomException.notFound("의사를 찾을 수 없습니다. ID: " + request.getDoctorId()));

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> CustomException.notFound("진료과를 찾을 수 없습니다. ID: " + request.getDepartmentId()));

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
    }
}
=======
                // 2. 의사 조회
                Doctor doctor = doctorRepository.findById(request.getDoctorId())
                                .orElseThrow(() -> new RuntimeException("의사를 찾을 수 없습니다."));

                // 3. 진료과 조회
                Department department = departmentRepository.findById(request.getDepartmentId())
                                .orElseThrow(() -> new RuntimeException("진료과를 찾을 수 없습니다."));

                // 4. 방문 접수는 오늘 날짜
                LocalDate reservationDate = LocalDate.now();

                // 5. 현재 시각 그대로 사용
                LocalTime now = LocalTime.now();
                String timeSlot = now.withSecond(0).withNano(0).toString();

                // 6. 예약번호 생성
                String reservationNumber = reservationNumberGenerator.generate(
                                reservationDate,
                                () -> reservationRepository.countByReservationDate(reservationDate));

                // 7. 예약 생성
                Reservation reservation = Reservation.create(
                                reservationNumber,
                                patient,
                                doctor,
                                department,
                                reservationDate,
                                timeSlot,
                                ReservationSource.WALKIN);

                // 8. 먼저 저장 → @PrePersist 실행 → status = RESERVED
                reservationRepository.save(reservation);

                // 9. 바로 접수 처리 → RECEIVED
                reservation.receive();
        }
}
>>>>>>> dev
