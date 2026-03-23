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

        // 진료과 ID에 해당하는 의사 목록을 DoctorDto 리스트로 반환 (AJAX 드롭다운용)
        public List<DoctorDto> getDoctorsByDepartment(Long departmentId) {
                return doctorRepository.findByDepartment_Id(departmentId)
                                .stream()
                                .map(DoctorDto::new)  // Doctor 엔티티 → DoctorDto 변환
                                .toList();
        }

        // 전체 진료과 목록을 DepartmentDto 리스트로 반환 (예약 폼 select 옵션용)
        public List<DepartmentDto> getDepartments() {
                return departmentRepository.findAll()
                                .stream()
                                .map(DepartmentDto::new)  // Department 엔티티 → DepartmentDto 변환
                                .toList();
        }

        // 예약번호로 단건 조회 → ReservationInfoDto로 변환 (트랜잭션 종료 후 LazyLoad 방지)
        public Optional<ReservationInfoDto> findByReservationNumber(String reservationNumber) {
                return reservationRepository.findByReservationNumber(reservationNumber)
                                .map(ReservationInfoDto::new);
        }

        // ID로 단건 조회 (fetch join 포함) → ReservationInfoDto로 변환
        public Optional<ReservationInfoDto> findById(Long id) {
                return reservationRepository.findByIdWithDetails(id)
                                .map(ReservationInfoDto::new);
        }

        // 특정 의사의 해당 날짜 예약된 시간 슬롯 목록 반환 (CANCELLED 제외, Flatpickr 비활성화용)
        public List<String> getBookedTimeSlots(Long doctorId, LocalDate date) {
                return reservationRepository.findBookedTimeSlots(doctorId, date, ReservationStatus.CANCELLED);
        }

        // 특정 예약 ID를 제외한 예약된 시간 슬롯 반환 (변경 시 본인 슬롯 제외하여 재선택 허용)
        public List<String> getBookedTimeSlots(Long doctorId, LocalDate date, Long excludeId) {
                return reservationRepository.findBookedTimeSlotsExcluding(
                                doctorId, date, ReservationStatus.CANCELLED, excludeId);
        }

        // 이름 + 전화번호로 예약 목록 조회 → ReservationInfoDto 리스트 반환
        public List<ReservationInfoDto> findByPhoneAndName(String phone, String name) {
                String trimmedName = name.trim();
                // 전화번호 정규화: 숫자만 추출 (하이픈·공백 제거)
                String normalizedPhone = phone.replaceAll("[^0-9]", "");
                return reservationRepository.findByNormalizedPhoneAndName(normalizedPhone, trimmedName)
                                .stream()
                                .map(ReservationInfoDto::new)
                                .toList();
        }

        @Transactional
        public ReservationCompleteInfo createReservation(CreateReservationRequest form) {
                // 동일 의사 + 날짜 + 시간에 CANCELLED 아닌 예약이 있으면 충돌 예외 (H-03)
                if (reservationRepository.existsByDoctor_IdAndReservationDateAndTimeSlotAndStatusNot(
                                form.doctorId(), form.reservationDate(), form.timeSlot(),
                                ReservationStatus.CANCELLED)) {
                        throw CustomException.conflict("DUPLICATE_RESERVATION", "이미 예약된 시간대입니다.");
                }

                // 전화번호로 기존 환자 조회 → 없으면 신규 환자 등록
                Patient patient = patientRepository.findByPhone(form.phone())
                                .orElseGet(() -> patientRepository.save(
                                                Patient.create(form.name(), form.phone(), null)));

                // 의사·진료과 존재 여부 확인
                Doctor doctor = doctorRepository.findById(form.doctorId())
                                .orElseThrow(() -> CustomException.notFound("의사를 찾을 수 없습니다."));
                Department department = departmentRepository.findById(form.departmentId())
                                .orElseThrow(() -> CustomException.notFound("진료과를 찾을 수 없습니다."));

                try {
                        // 예약번호 생성 (RES-YYYYMMDD-XXX 형식) 및 예약 엔티티 저장
                        String reservationNumber = reservationNumberGenerator.generate(
                                        form.reservationDate(),
                                        () -> reservationRepository.countByReservationDate(form.reservationDate()));
                        Reservation reservation = Reservation.create(
                                        reservationNumber, patient, doctor, department,
                                        form.reservationDate(), form.timeSlot(),
                                        ReservationSource.ONLINE);
                        reservationRepository.save(reservation);
                        // flush로 DB 제약 조건 위반을 즉시 감지
                        reservationRepository.flush();

                        // 완료 화면 표시에 필요한 정보만 DTO로 포장하여 반환 (트랜잭션 종료 후 LazyLoad 방지)
                        return new ReservationCompleteInfo(
                                        reservationNumber,
                                        patient.getName(),
                                        department.getName(),
                                        doctor.getStaff().getName(),
                                        form.reservationDate().toString(),
                                        form.timeSlot());
                } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        // 동시 요청으로 인한 DB 레벨 중복 예약 처리
                        throw CustomException.conflict("DUPLICATE_RESERVATION", "이미 예약된 시간대이거나 중복된 예약번호입니다.");
                }
        }

        @Transactional
        public ReservationCompleteInfo cancelReservation(Long id, String phone) {
                Reservation reservation = reservationRepository.findById(id)
                                .orElseThrow(() -> CustomException.notFound("예약을 찾을 수 없습니다."));

                // H-01: 소유권 검증 — 입력 전화번호와 예약 전화번호 모두 정규화 후 비교
                String normalizedInput = phone.replaceAll("[^0-9]", "");
                String normalizedStored = reservation.getPatient().getPhone().replaceAll("[^0-9]", "");
                if (!normalizedInput.equals(normalizedStored)) {
                        throw CustomException.forbidden("예약 소유자가 아닙니다.");
                }

                // 취소 전 완료 화면 표시 정보를 미리 DTO로 추출 (cancelFully 이후 LazyLoad 불가)
                ReservationCompleteInfo info = new ReservationCompleteInfo(
                                reservation.getReservationNumber(),
                                reservation.getPatient().getName(),
                                reservation.getDepartment().getName(),
                                reservation.getDoctor().getStaff().getName(),
                                reservation.getReservationDate().toString(),
                                reservation.getTimeSlot());

                // 예약 상태를 CANCELLED로 변경 + 슬롯 해제
                reservation.cancelFully(null);
                return info;
        }

        @Transactional
        public ReservationCompleteInfo updateReservation(Long id, String phone, UpdateReservationRequest form) {
                // H-01 + H-03: 비관적 락으로 기존 예약 조회 (동시 수정 방지)
                Reservation old = reservationRepository.findByIdForUpdate(id)
                                .orElseThrow(() -> CustomException.notFound("예약을 찾을 수 없습니다."));

                // H-01: 소유권 검증 — 입력 전화번호와 예약 전화번호 모두 정규화 후 비교
                String normalizedInput = phone.replaceAll("[^0-9]", "");
                String normalizedStored = old.getPatient().getPhone().replaceAll("[^0-9]", "");
                if (!normalizedInput.equals(normalizedStored)) {
                        throw CustomException.forbidden("예약 소유자가 아닙니다.");
                }

                // 새 슬롯 중복 체크 (기존 예약은 이후 cancelFully로 해제되므로 현재 시점에 충돌로 처리)
                if (reservationRepository.existsByDoctor_IdAndReservationDateAndTimeSlotAndStatusNot(
                                form.doctorId(), form.reservationDate(), form.timeSlot(),
                                ReservationStatus.CANCELLED)) {
                        throw CustomException.conflict("DUPLICATE_RESERVATION", "이미 예약된 시간대입니다.");
                }

                // 기존 환자 정보 유지, 기존 예약 취소 처리
                Patient patient = old.getPatient();
                old.cancelFully(null);

                // 새 의사·진료과 존재 여부 확인
                Doctor doctor = doctorRepository.findById(form.doctorId())
                                .orElseThrow(() -> CustomException.notFound("의사를 찾을 수 없습니다."));
                Department department = departmentRepository.findById(form.departmentId())
                                .orElseThrow(() -> CustomException.notFound("진료과를 찾을 수 없습니다."));

                try {
                        // 새 예약번호 생성 및 새 예약 엔티티 저장
                        String reservationNumber = reservationNumberGenerator.generate(
                                        form.reservationDate(),
                                        () -> reservationRepository.countByReservationDate(form.reservationDate()));
                        Reservation newReservation = Reservation.create(
                                        reservationNumber, patient, doctor, department,
                                        form.reservationDate(), form.timeSlot(), ReservationSource.ONLINE);
                        reservationRepository.save(newReservation);
                        // flush로 DB 제약 조건 위반을 즉시 감지
                        reservationRepository.flush();

                        // 완료 화면 표시에 필요한 정보만 DTO로 포장하여 반환
                        return new ReservationCompleteInfo(
                                        reservationNumber,
                                        patient.getName(),
                                        department.getName(),
                                        doctor.getStaff().getName(),
                                        form.reservationDate().toString(),
                                        form.timeSlot());
                } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        // 동시 요청으로 인한 DB 레벨 중복 예약 처리
                        throw CustomException.conflict("DUPLICATE_RESERVATION", "이미 예약된 시간대이거나 중복된 예약번호입니다.");
                }
        }
}
