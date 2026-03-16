package com.smartclinic.hms.reservation.reservation;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartclinic.hms.common.exception.CustomException;
import com.smartclinic.hms.doctor.DoctorDto;
import com.smartclinic.hms.doctor.DoctorRepository;
import com.smartclinic.hms.domain.Department;
import com.smartclinic.hms.domain.Doctor;
import com.smartclinic.hms.domain.Patient;
import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationSource;
import com.smartclinic.hms.domain.ReservationStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

        private final DoctorRepository doctorRepository;
        private final PatientRepository patientRepository;
        private final ReservationRepository reservationRepository;
        private final DepartmentRepository departmentRepository;
        private final Clock clock;

        public List<DoctorDto> getDoctorsByDepartment(Long departmentId) {
                return doctorRepository.findByDepartment_Id(departmentId)
                                .stream()
                                .map(DoctorDto::new)
                                .toList();
        }

        public List<DepartmentDto> getDepartments() {
                return departmentRepository.findAll()
                                .stream()
                                .map(DepartmentDto::new)
                                .toList();
        }

        public Optional<ReservationInfoDto> findByReservationNumber(String reservationNumber) {
                return reservationRepository.findByReservationNumber(reservationNumber)
                                .map(ReservationInfoDto::new);
        }

        public Optional<ReservationInfoDto> findById(Long id) {
                return reservationRepository.findByIdWithDetails(id)
                                .map(ReservationInfoDto::new);
        }

        public List<ReservationInfoDto> findByPhoneAndName(String phone, String name) {
                String trimmedName = name.trim();
                String normalizedPhone = phone.replaceAll("[^0-9]", "");
                return reservationRepository.findByNormalizedPhoneAndName(normalizedPhone, trimmedName)
                                .stream()
                                .map(ReservationInfoDto::new)
                                .toList();
        }

        // RES-YYYYMMDD-XXX-NNNN 형식 예약번호 생성
        public String generateReservationNumber() {
                LocalDateTime now = LocalDateTime.now(clock);
                LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusNanos(1);

                long count = reservationRepository.countByCreatedAtBetween(startOfMonth, endOfMonth) + 1;
                String dateStr = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                String nanoStr = String.format("%04d", System.nanoTime() % 10000);

                return String.format("RES-%s-%03d-%s", dateStr, count, nanoStr);
        }

        @Transactional
        public ReservationCompleteInfo createReservation(ReservationCreateForm form) {
                if (reservationRepository.existsByDoctor_IdAndReservationDateAndTimeSlotAndStatusNot(
                                form.doctorId(), form.reservationDate(), form.timeSlot(),
                                ReservationStatus.CANCELLED)) {
                        throw CustomException.conflict("DUPLICATE_RESERVATION", "이미 예약된 시간대입니다.");
                }

                Patient patient = patientRepository.findByPhone(form.phone())
                                .orElseGet(() -> patientRepository.save(
                                                Patient.create(form.name(), form.phone(), null)));

                Doctor doctor = doctorRepository.findById(form.doctorId())
                                .orElseThrow(() -> CustomException.notFound("의사를 찾을 수 없습니다."));
                Department department = departmentRepository.findById(form.departmentId())
                                .orElseThrow(() -> CustomException.notFound("진료과를 찾을 수 없습니다."));

                try {
                        String reservationNumber = generateReservationNumber();
                        Reservation reservation = Reservation.create(
                                        reservationNumber, patient, doctor, department,
                                        form.reservationDate(), form.timeSlot(),
                                        ReservationSource.ONLINE);
                        reservationRepository.save(reservation);
                        reservationRepository.flush();

                        return new ReservationCompleteInfo(
                                        reservationNumber,
                                        patient.getName(),
                                        department.getName(),
                                        doctor.getStaff().getName(),
                                        form.reservationDate().toString(),
                                        form.timeSlot());
                } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        throw CustomException.conflict("DUPLICATE_RESERVATION", "이미 예약된 시간대이거나 중복된 예약번호입니다.");
                }
        }

        @Transactional
        public ReservationCompleteInfo cancelReservation(Long id) {
                Reservation reservation = reservationRepository.findById(id)
                                .orElseThrow(() -> CustomException.notFound("예약을 찾을 수 없습니다."));

                ReservationCompleteInfo info = new ReservationCompleteInfo(
                                reservation.getReservationNumber(),
                                reservation.getPatient().getName(),
                                reservation.getDepartment().getName(),
                                reservation.getDoctor().getStaff().getName(),
                                reservation.getReservationDate().toString(),
                                reservation.getTimeSlot());

                reservation.cancel();
                return info;
        }

        @Transactional
        public ReservationCompleteInfo updateReservation(Long id, ReservationUpdateForm form) {
                // 1. 새 슬롯 중복 체크 (cancel 이전에 먼저 수행 — TOCTOU 방지)
                if (reservationRepository.existsByDoctor_IdAndReservationDateAndTimeSlotAndStatusNot(
                                form.doctorId(), form.reservationDate(), form.timeSlot(),
                                ReservationStatus.CANCELLED)) {
                        throw CustomException.conflict("DUPLICATE_RESERVATION", "이미 예약된 시간대입니다.");
                }

                // 2. 기존 예약 취소
                Reservation old = reservationRepository.findById(id)
                                .orElseThrow(() -> CustomException.notFound("예약을 찾을 수 없습니다."));
                Patient patient = old.getPatient();
                old.cancel();

                // 3. 신규 예약 생성
                Doctor doctor = doctorRepository.findById(form.doctorId())
                                .orElseThrow(() -> CustomException.notFound("의사를 찾을 수 없습니다."));
                Department department = departmentRepository.findById(form.departmentId())
                                .orElseThrow(() -> CustomException.notFound("진료과를 찾을 수 없습니다."));

                try {
                        String reservationNumber = generateReservationNumber();
                        Reservation newReservation = Reservation.create(
                                        reservationNumber, patient, doctor, department,
                                        form.reservationDate(), form.timeSlot(), ReservationSource.ONLINE);
                        reservationRepository.save(newReservation);
                        reservationRepository.flush();

                        return new ReservationCompleteInfo(
                                        reservationNumber,
                                        patient.getName(),
                                        department.getName(),
                                        doctor.getStaff().getName(),
                                        form.reservationDate().toString(),
                                        form.timeSlot());
                } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        throw CustomException.conflict("DUPLICATE_RESERVATION", "이미 예약된 시간대이거나 중복된 예약번호입니다.");
                }
        }
}
