package com.smartclinic.hms.staff.walkin;

import java.time.LocalDate;
import java.util.List;

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
import com.smartclinic.hms.domain.ReservationStatus;
import com.smartclinic.hms.reservation.reservation.DepartmentRepository;
import com.smartclinic.hms.reservation.reservation.PatientRepository;
import com.smartclinic.hms.reservation.reservation.ReservationRepository;
import com.smartclinic.hms.staff.reception.ReceptionService;
import com.smartclinic.hms.staff.reception.dto.ReceptionUpdateRequest;
import com.smartclinic.hms.staff.walkin.dto.WalkinRequestDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalkinService {

        private final ReservationRepository reservationRepository;
        private final ReceptionService receptionService;
        private final PatientRepository patientRepository;
        private final DoctorRepository doctorRepository;
        private final DepartmentRepository departmentRepository;
        private final ReservationNumberGenerator reservationNumberGenerator;

        // 방문 접수 생성
        @Transactional
        public boolean createWalkin(WalkinRequestDto request) {

                // 1. 기존 환자 조회 (없으면 신규 생성)
                Patient patient = patientRepository.findByPhone(request.getPhone()).orElse(null);
                boolean nameMismatch = false;

                if (patient == null) {
                        patient = patientRepository.save(
                                        Patient.create(
                                                        request.getName(),
                                                        request.getPhone(),
                                                        null));
                } else if (!patient.getName().equals(request.getName())) {
                        nameMismatch = true;
                }

                Doctor doctor = doctorRepository.findById(request.getDoctorId())
                                .orElseThrow(() -> CustomException
                                                .notFound("의사를 찾을 수 없습니다. ID: " + request.getDoctorId()));

                Department department = departmentRepository.findById(request.getDepartmentId())
                                .orElseThrow(() -> CustomException
                                                .notFound("진료과를 찾을 수 없습니다. ID: " + request.getDepartmentId()));

                // 중복 접수 검증 (request.getDate()가 이미 LocalDate 타입이므로 parse 불필요!)
                LocalDate reservationDate = request.getDate();
                List<Reservation> existingReservations = reservationRepository.findTodayExcludingStatus(reservationDate,
                                ReservationStatus.CANCELLED);
                boolean isDuplicate = existingReservations.stream()
                                .anyMatch(r -> r.getDoctor().getId().equals(doctor.getId())
                                                && r.getTimeSlot().equals(request.getTime()));
                if (isDuplicate) {
                        throw CustomException.conflict("DUPLICATE_RESERVATION",
                                        "해당 의사 선생님의 선택하신 시간대에는 이미 접수된 정보가 있습니다.");
                }

                String reservationNumber = reservationNumberGenerator.generate(
                                reservationDate,
                                () -> reservationRepository.countByReservationDate(reservationDate));

                Reservation reservation = Reservation.create(
                                reservationNumber,
                                patient,
                                doctor,
                                department,
                                reservationDate,
                                request.getTime(),
                                ReservationSource.WALKIN);

                // 방문 접수는 생성 즉시 '진료 대기'(RECEIVED) 상태로 변경
                reservation.receive();

                reservationRepository.save(reservation);

                return nameMismatch;
        }
}
