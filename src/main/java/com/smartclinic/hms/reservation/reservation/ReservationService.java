package com.smartclinic.hms.reservation.reservation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartclinic.hms.common.exception.CustomException;
import com.smartclinic.hms.common.util.ReservationNumberGenerator;
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
        private final ReservationNumberGenerator reservationNumberGenerator;

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

        public List<String> getBookedTimeSlots(Long doctorId, LocalDate date) {
                return reservationRepository.findBookedTimeSlots(doctorId, date, ReservationStatus.CANCELLED);
        }

        public List<String> getBookedTimeSlots(Long doctorId, LocalDate date, Long excludeId) {
                return reservationRepository.findBookedTimeSlotsExcluding(
                                doctorId, date, ReservationStatus.CANCELLED, excludeId);
        }

        public List<ReservationInfoDto> findByPhoneAndName(String phone, String name) {
                String trimmedName = name.trim();
                String normalizedPhone = phone.replaceAll("[^0-9]", "");
                return reservationRepository.findByNormalizedPhoneAndName(normalizedPhone, trimmedName)
                                .stream()
                                .map(ReservationInfoDto::new)
                                .toList();
        }

        @Transactional
        public ReservationCompleteInfo createReservation(CreateReservationRequest form) {
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
                        String reservationNumber = reservationNumberGenerator.generate(
                                        form.reservationDate(),
                                        () -> reservationRepository.countByReservationDate(form.reservationDate()));
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
        public ReservationCompleteInfo cancelReservation(Long id, String phone) {
                Reservation reservation = reservationRepository.findById(id)
                                .orElseThrow(() -> CustomException.notFound("예약을 찾을 수 없습니다."));

                // H-01: 소유권 검증 (전화번호 일치 확인)
                String normalizedInput = phone.replaceAll("[^0-9]", "");
                String normalizedStored = reservation.getPatient().getPhone().replaceAll("[^0-9]", "");
                if (!normalizedInput.equals(normalizedStored)) {
                        throw CustomException.forbidden("예약 소유자가 아닙니다.");
                }

                ReservationCompleteInfo info = new ReservationCompleteInfo(
                                reservation.getReservationNumber(),
                                reservation.getPatient().getName(),
                                reservation.getDepartment().getName(),
                                reservation.getDoctor().getStaff().getName(),
                                reservation.getReservationDate().toString(),
                                reservation.getTimeSlot());

                reservation.cancelFully(null);
                return info;
        }

        @Transactional
        public ReservationCompleteInfo updateReservation(Long id, String phone, UpdateReservationRequest form) {
                // H-01 + H-03: 비관적 락으로 기존 예약 조회 후 소유권 검증 (동시 수정 방지)
                Reservation old = reservationRepository.findByIdForUpdate(id)
                                .orElseThrow(() -> CustomException.notFound("예약을 찾을 수 없습니다."));

                String normalizedInput = phone.replaceAll("[^0-9]", "");
                String normalizedStored = old.getPatient().getPhone().replaceAll("[^0-9]", "");
                if (!normalizedInput.equals(normalizedStored)) {
                        throw CustomException.forbidden("예약 소유자가 아닙니다.");
                }

                // 새 슬롯 중복 체크
                if (reservationRepository.existsByDoctor_IdAndReservationDateAndTimeSlotAndStatusNot(
                                form.doctorId(), form.reservationDate(), form.timeSlot(),
                                ReservationStatus.CANCELLED)) {
                        throw CustomException.conflict("DUPLICATE_RESERVATION", "이미 예약된 시간대입니다.");
                }

                Patient patient = old.getPatient();
                old.cancelFully(null);

                Doctor doctor = doctorRepository.findById(form.doctorId())
                                .orElseThrow(() -> CustomException.notFound("의사를 찾을 수 없습니다."));
                Department department = departmentRepository.findById(form.departmentId())
                                .orElseThrow(() -> CustomException.notFound("진료과를 찾을 수 없습니다."));

                try {
                        String reservationNumber = reservationNumberGenerator.generate(
                                        form.reservationDate(),
                                        () -> reservationRepository.countByReservationDate(form.reservationDate()));
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
