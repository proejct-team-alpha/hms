package com.smartclinic.hms.reservation.reservation;

// [W2-#4 작업 목록]
// DONE 1. PatientRepository, ReservationRepository, DepartmentRepository 필드 추가
// DONE 2. createReservation() 구현 (Patient 조회/생성 → Doctor/Department 조회 → Reservation 저장)

// [W2-#5 작업 목록]
// DONE 1. createReservation() — 중복 예약 체크 추가
// DONE 2. findByReservationNumber(String) — 예약번호로 단건 조회
// DONE 3. findByPhoneAndName(String, String) — 전화번호+이름으로 목록 조회
// DONE 4. cancelReservation(Long) — 상태 CANCELLED 변경
// DONE 5. updateReservation(Long, ReservationUpdateForm) — cancel + create 조합
// DONE 6. getDepartments() — 진료과 전체 목록 조회

// [W2-#5.1 작업 목록]
// DONE 1. generateReservationNumber() — RES-YYYYMMDD-XXX 형식 (당월 누적)
// DONE 2. findByReservationNumber(), findByPhoneAndName() → ReservationInfoDto 반환으로 수정

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        public List<ReservationInfoDto> findByPhoneAndName(String phone, String name) {
                // 이름 공백 제거, 전화번호 숫자만 추출 (010-1111-2222 → 01011112222)
                String trimmedName = name.trim();
                String normalizedPhone = phone.replaceAll("[^0-9]", "");
                return reservationRepository.findByNormalizedPhoneAndName(normalizedPhone, trimmedName)
                                .stream()
                                .map(ReservationInfoDto::new)
                                .toList();
        }

        // RES-YYYYMMDD-XXX-NNNN 형식 예약번호 생성 (당월 누적 카운트 + 나노초 일부 조합으로 동시성 강화)
        public String generateReservationNumber() {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusNanos(1);
                
                // 기존 방식: 당월 카운트 + 1
                long count = reservationRepository.countByCreatedAtBetween(startOfMonth, endOfMonth) + 1;
                String dateStr = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                
                // 동시성 충돌 방지를 위해 나노초 뒷자리 4개를 추가 (H2 이슈 대응)
                String nanoStr = String.format("%04d", System.nanoTime() % 10000);
                
                return String.format("RES-%s-%03d-%s", dateStr, count, nanoStr);
        }

        @Transactional
        public ReservationCompleteInfo createReservation(ReservationCreateForm form) {
                // 1. 조기 차단: 중복 예약 체크 (CANCELLED 제외)
                if (reservationRepository.existsByDoctor_IdAndReservationDateAndTimeSlotAndStatusNot(
                                form.doctorId(), form.reservationDate(), form.timeSlot(),
                                ReservationStatus.CANCELLED)) {
                        throw new IllegalStateException("이미 예약된 시간대입니다.");
                }

                // 2. 전화번호로 Patient 조회, 없으면 신규 생성
                Patient patient = patientRepository.findByPhone(form.phone())
                                .orElseGet(() -> patientRepository.save(
                                                Patient.create(form.name(), form.phone(), null)));

                // 3. Doctor, Department 조회
                Doctor doctor = doctorRepository.findById(form.doctorId())
                                .orElseThrow(() -> new IllegalArgumentException("의사를 찾을 수 없습니다."));
                Department department = departmentRepository.findById(form.departmentId())
                                .orElseThrow(() -> new IllegalArgumentException("진료과를 찾을 수 없습니다."));

                // 4. 예약번호 생성 및 저장 (H3 TOCTOU 대응을 위해 DB Unique 제약 조건 활용)
                try {
                        String reservationNumber = generateReservationNumber();
                        Reservation reservation = Reservation.create(
                                        reservationNumber, patient, doctor, department,
                                        form.reservationDate(), form.timeSlot(),
                                        ReservationSource.ONLINE);
                        
                        reservationRepository.save(reservation);
                        // 즉시 플러시하여 DB 제약 조건 위반을 조기에 감지
                        reservationRepository.flush();

                        return new ReservationCompleteInfo(
                                        reservationNumber,
                                        patient.getName(),
                                        department.getName(),
                                        doctor.getStaff().getName(),
                                        form.reservationDate().toString(),
                                        form.timeSlot());
                } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        // DB Unique 제약 조건(uk_reservation_doctor_date_slot) 위반 시 발생
                        throw new IllegalStateException("이미 예약된 시간대이거나 중복된 예약번호입니다.");
                }
        }

        @Transactional
        public ReservationCompleteInfo cancelReservation(Long id) {
                Reservation reservation = reservationRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));

                // 취소 전에 정보 저장
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
                // 1. 기존 예약 취소
                Reservation old = reservationRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));
                Patient patient = old.getPatient();
                old.cancel();

                // 2. 조기 차단: 새 슬롯 중복 체크
                if (reservationRepository.existsByDoctor_IdAndReservationDateAndTimeSlotAndStatusNot(
                                form.doctorId(), form.reservationDate(), form.timeSlot(),
                                ReservationStatus.CANCELLED)) {
                        throw new IllegalStateException("이미 예약된 시간대입니다.");
                }

                // 3. 신규 예약 생성
                Doctor doctor = doctorRepository.findById(form.doctorId())
                                .orElseThrow(() -> new IllegalArgumentException("의사를 찾을 수 없습니다."));
                Department department = departmentRepository.findById(form.departmentId())
                                .orElseThrow(() -> new IllegalArgumentException("진료과를 찾을 수 없습니다."));

                try {
                        String reservationNumber = generateReservationNumber();
                        Reservation newReservation = Reservation.create(
                                        reservationNumber, patient, doctor, department,
                                        form.reservationDate(), form.timeSlot(), ReservationSource.ONLINE);
                        reservationRepository.save(newReservation);
                        // 즉시 플러시하여 DB 제약 조건 위반 감지
                        reservationRepository.flush();

                        return new ReservationCompleteInfo(
                                        reservationNumber,
                                        patient.getName(),
                                        department.getName(),
                                        doctor.getStaff().getName(),
                                        form.reservationDate().toString(),
                                        form.timeSlot());
                } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        throw new IllegalStateException("이미 예약된 시간대이거나 중복된 예약번호입니다.");
                }
        }
}
