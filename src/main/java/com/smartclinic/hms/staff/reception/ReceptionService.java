package com.smartclinic.hms.staff.reception;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.smartclinic.hms.staff.dto.StaffDashboardDto;
import com.smartclinic.hms.staff.dto.StaffDepartmentOptionDto;
import com.smartclinic.hms.staff.dto.StaffDoctorOptionDto;
import com.smartclinic.hms.staff.dto.StaffReservationDto;
import com.smartclinic.hms.staff.dto.StaffStatusFilter;
import com.smartclinic.hms.staff.reception.dto.PatientInfoUpdateRequest;
import com.smartclinic.hms.staff.reception.dto.ReceptionUpdateRequest;
import com.smartclinic.hms.staff.reservation.dto.PhoneReservationRequestDto;
import com.smartclinic.hms.common.exception.CustomException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReceptionService {

    private final ReservationRepository reservationRepository;
    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final PatientRepository patientRepository;
    private final com.smartclinic.hms.doctor.treatment.DoctorTreatmentRecordRepository treatmentRecordRepository;
    private final ReservationNumberGenerator reservationNumberGenerator;

    // 전화 예약 생성
    @Transactional
    public boolean createPhoneReservation(PhoneReservationRequestDto request) {

        String phone = request.getPhone().trim();

        // 1️ 환자 조회
        Patient patient = patientRepository.findByPhone(phone).orElse(null);
        boolean nameMismatch = false;

        // 2️ 없으면 신규 환자 생성
        if (patient == null) {
            patient = Patient.create(
                    request.getName(),
                    phone,
                    request.getEmail());
            // [기능 구현] 신규 환자 생성 시 주민번호 정보가 있으면 저장
            if (request.getBirthInfo() != null && !request.getBirthInfo().isBlank()) {
                patient.updateMedicalInfo(request.getBirthInfo(), request.getVisitReason());
            }
            patientRepository.save(patient);
        } else {
            // [기능 구현] 기존 환자라도 전화 예약 시 입력된 내원 사유 및 주민번호(정보가 있을 때만)를 최신 정보로 업데이트
            if (!patient.getName().equals(request.getName())) {
                nameMismatch = true;
            }
            String finalBirth = (request.getBirthInfo() != null && !request.getBirthInfo().isBlank())
                    ? request.getBirthInfo()
                    : patient.getBirthInfo();
            String finalReason = (request.getVisitReason() != null && !request.getVisitReason().isBlank())
                    ? request.getVisitReason()
                    : patient.getVisitReason();
            patient.updateMedicalInfo(finalBirth, finalReason);
        }

        // 3️ 의사 조회
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> CustomException.notFound("의사를 찾을 수 없습니다."));

        // 4️ 진료과 조회
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> CustomException.notFound("진료과를 찾을 수 없습니다."));

        // 5️ 예약 날짜
        LocalDate reservationDate = LocalDate.parse(request.getDate());

        // 🚨 중복 예약 검증
        List<Reservation> existingReservations = reservationRepository.findTodayExcludingStatus(reservationDate,
                ReservationStatus.CANCELLED);
        boolean isDuplicate = existingReservations.stream()
                .anyMatch(r -> r.getDoctor().getId().equals(doctor.getId())
                        && r.getTimeSlot().equals(request.getTime()));
        if (isDuplicate) {
            throw CustomException.conflict("DUPLICATE_RESERVATION", "해당 의사 선생님의 선택하신 시간대에는 이미 예약된 정보가 있습니다.");
        }

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
        return nameMismatch;
    }

    // 접수 처리
    @Transactional
    public void receive(ReceptionUpdateRequest request) {

        Reservation reservation = reservationRepository.findById(request.getReservationId())
                .orElseThrow(() -> CustomException.notFound("예약을 찾을 수 없습니다."));
        reservation.receive();
    }

    // [주석] 환자 상세 정보 업데이트 (예약 및 환자 엔티티 모두 반영)
    @Transactional
    public void updatePatientInfo(PatientInfoUpdateRequest request) {
        // 1. 예약 정보 조회
        Reservation reservation = reservationRepository.findById(request.getReservationId())
                .orElseThrow(() -> CustomException.notFound("예약을 찾을 수 없습니다."));

        // 2. 환자 정보 조회 및 업데이트
        Patient patient = reservation.getPatient();

        // [기능 구현] 수정 가능 상태 여부 판단 (예약/대기 중일 때만 핵심 진료 정보 수정 가능)
        boolean canEditReception = !reservation.isPaid() && !reservation.isTreatmentCompleted() &&
                (reservation.getStatus() == ReservationStatus.RESERVED
                        || reservation.getStatus() == ReservationStatus.RECEIVED);

        // [주석] 환자의 기본 정보(이메일, 주소, 메모)는 항상 업데이트 가능
        patient.updateInfo(patient.getName(), patient.getPhone(), request.getEmail(), request.getAddress(),
                request.getNote());

        // [기능 구현] 내원 사유는 수정 가능할 때만 업데이트, 주민번호는 항상 업데이트 허용
        String finalVisitReason = canEditReception ? request.getVisitReason() : patient.getVisitReason();
        patient.updateMedicalInfo(request.getBirthInfo(), finalVisitReason);

        // 3. 진료과 및 의사 정보 업데이트 (수정 가능 상태일 때만 반영)
        if (canEditReception && request.getDeptId() != null && request.getDoctorId() != null) {
            Department dept = departmentRepository.findById(request.getDeptId())
                    .orElseThrow(() -> CustomException.notFound("진료과를 찾을 수 없습니다."));
            Doctor doctor = doctorRepository.findById(request.getDoctorId())
                    .orElseThrow(() -> CustomException.notFound("의사를 찾을 수 없습니다."));

            reservation.updateReceptionInfo(dept, doctor);
        }
    }

    /**
     * 날짜별 예약 목록 조회 (다중 필터링 지원)
     * 
     * @param date      조회 날짜 (null이면 오늘 이후 전체)
     * @param status    예약 상태 (문자열)
     * @param query     검색어 (환자명, 전화번호 등)
     * @param deptIds   다중 선택된 진료과 ID 리스트
     * @param doctorIds 다중 선택된 의사 ID 리스트
     * @param source    예약 경로 (ONLINE, PHONE, WALKIN)
     * @return 필터링된 예약 DTO 리스트
     */
    public List<StaffReservationDto> getReservations(LocalDate date, String status, String query, List<Long> deptIds,
            List<Long> doctorIds, String source) {
        List<Reservation> reservations;
        if (date == null) {
            LocalDate today = LocalDate.now();
            if (status == null || status.isBlank()) {
                reservations = reservationRepository.findFromDateAll(today);
            } else {
                reservations = reservationRepository.findFromDateByStatus(today, ReservationStatus.valueOf(status));
            }
        } else {
            if (status == null || status.isBlank()) {
                reservations = reservationRepository.findTodayAll(date);
            } else {
                reservations = reservationRepository.findTodayByStatus(date, ReservationStatus.valueOf(status));
            }
        }

        return reservations.stream()
                .filter(r -> {
                    // 검색어 필터링 (환자명, 전화번호, 진료과, 전문의)
                    if (query != null && !query.isBlank()) {
                        String q = query.toLowerCase();
                        boolean matches = r.getPatient().getName().toLowerCase().contains(q) ||
                                r.getPatient().getPhone().contains(q) ||
                                r.getDepartment().getName().toLowerCase().contains(q) ||
                                r.getDoctor().getStaff().getName().toLowerCase().contains(q);
                        if (!matches)
                            return false;
                    }
                    // 다중 진료과 필터 적용
                    if (deptIds != null && !deptIds.isEmpty() && !deptIds.contains(r.getDepartment().getId()))
                        return false;
                    // 다중 전문의 필터 적용
                    if (doctorIds != null && !doctorIds.isEmpty() && !doctorIds.contains(r.getDoctor().getId()))
                        return false;
                    // 예약 구분 필터
                    if (source != null && !source.isBlank() && !r.getSource().name().equals(source))
                        return false;

                    return true;
                })
                .map(r -> {
                    // 환자의 진료 완료 건수를 조회하여 초재진 여부를 판별합니다.
                    long completedCount = reservationRepository.countByPatient_IdAndStatus(r.getPatient().getId(),
                            ReservationStatus.COMPLETED);
                    return new StaffReservationDto(r, completedCount);
                })
                .collect(Collectors.toList());
    }

    /**
     * 상태 필터 탭 목록 생성 (명칭 변경: 접수 대기 -> 예약)
     */
    public List<StaffStatusFilter> getStatusFilters(String selected, String date, String query, List<Long> deptIds,
            List<Long> doctorIds, String source) {
        String s = (selected == null) ? "" : selected;
        // 단일 ID만 받는 기존 DTO 구조상 첫 번째 값을 전달 (기존 호환성 유지)
        Long dId = (deptIds != null && !deptIds.isEmpty()) ? deptIds.get(0) : null;
        Long docId = (doctorIds != null && !doctorIds.isEmpty()) ? doctorIds.get(0) : null;

        return List.of(
                new StaffStatusFilter("전체", "", s, date, query, dId, docId, source),
                new StaffStatusFilter("예약", "RESERVED", s, date, query, dId, docId, source),
                new StaffStatusFilter("진료 대기", "RECEIVED", s, date, query, dId, docId, source),
                new StaffStatusFilter("진료 완료", "COMPLETED", s, date, query, dId, docId, source),
                new StaffStatusFilter("취소", "CANCELLED", s, date, query, dId, docId, source));
    }

    /**
     * 예약 상세 조회
     */
    public StaffReservationDto getDetail(Long id) {
        Reservation r = reservationRepository.findByIdWithDetails(id)
                .orElseThrow(() -> CustomException.notFound("예약을 찾을 수 없습니다."));

        // 1. 초재진 판별을 위한 진료 완료 건수 조회
        long completedCount = reservationRepository.countByPatient_IdAndStatus(r.getPatient().getId(),
                ReservationStatus.COMPLETED);

        // 2. 환자의 전체 예약 히스토리 조회 (최신순, 모든 상태 포함)
        List<com.smartclinic.hms.domain.PatientHistoryDto> history = reservationRepository
                .findByPatient_IdOrderByReservationDateDesc(r.getPatient().getId())
                .stream()
                .filter(res -> !res.getId().equals(id)) // 현재 예약은 히스토리에서 제외
                .map(res -> {
                    // 진료 완료된 경우에만 진료 기록을 조회하여 DTO 생성
                    com.smartclinic.hms.domain.TreatmentRecord tr = null;
                    if (res.getStatus() == ReservationStatus.COMPLETED) {
                        tr = treatmentRecordRepository.findByReservation_Id(res.getId()).orElse(null);
                    }
                    return new com.smartclinic.hms.domain.PatientHistoryDto(res, tr);
                })
                .toList();

        return new StaffReservationDto(r, completedCount, history);
    }

    /**
     * 예약 취소: {@code Reservation#cancel} — RECEIVED→RESERVED 롤백, RESERVED→CANCELLED.
     * IN_TREATMENT 등 전이 불가 시
     * {@link com.smartclinic.hms.common.exception.CustomException#invalidStatusTransition(String)}.
     */
    @Transactional
    public Reservation cancel(Long id, String reason) {
        Reservation r = reservationRepository.findById(id)
                .orElseThrow(() -> CustomException.notFound("예약을 찾을 수 없습니다."));
        try {
            r.cancelFully(reason); // 원무과 취소: 상태 무관하게 바로 CANCELLED → 슬롯 즉시 해제
        } catch (IllegalStateException ex) {
            throw CustomException.invalidStatusTransition(ex.getMessage());
        }
        return r;
    }

    /**
     * 수납 완료 처리
     */
    @Transactional
    public void completePayment(Long id) {
        Reservation r = reservationRepository.findById(id)
                .orElseThrow(() -> CustomException.notFound("예약을 찾을 수 없습니다."));
        r.pay();
    }

    /**
     * 대시보드 통계 및 최근 예약 목록
     */
    public StaffDashboardDto getDashboard() {
        LocalDate today = LocalDate.now();
        List<Reservation> all = reservationRepository.findTodayExcludingStatus(today, ReservationStatus.CANCELLED);
        int total = all.size();
        int waiting = (int) all.stream().filter(r -> r.getStatus() == ReservationStatus.RESERVED).count();
        int received = (int) all.stream().filter(r -> r.getStatus() == ReservationStatus.RECEIVED).count();
        // [기능 구현] 오늘 수납이 완료된 건수 계산
        int paid = (int) all.stream().filter(Reservation::isPaid).count();
        List<StaffReservationDto> recent = all.stream()
                .limit(5)
                .map(r -> {
                    // 환자의 초재진 판별을 위해 진료 완료 건수를 조회합니다.
                    long completedCount = reservationRepository.countByPatient_IdAndStatus(r.getPatient().getId(),
                            ReservationStatus.COMPLETED);
                    return new StaffReservationDto(r, completedCount);
                })
                .collect(Collectors.toList());

        // [기능 수정] 시간대별 통계 생성 (09시 ~ 18시): 예약/접수 개별 카운트 대신 전체 방문 환자 합계 중심으로 단순화
        java.util.List<com.smartclinic.hms.staff.dto.StaffHourlyStatDto> hourlyStats = new java.util.ArrayList<>();
        for (int hour = 9; hour <= 18; hour++) {
            String label = String.format("%02d:00", hour);
            final int h = hour;

            // 해당 시간대에 배정된 취소되지 않은 모든 예약(접수 포함) 건수를 합산
            long totalCountForHour = all.stream()
                    .filter(r -> r.getTimeSlot().startsWith(String.format("%02d:", h)))
                    .count();

            // 차트 표시를 위해 totalCount 중심으로 DTO 생성 (기존 호환성 유지를 위해 res/rec는 0으로 처리하거나 단순화 가능)
            hourlyStats.add(new com.smartclinic.hms.staff.dto.StaffHourlyStatDto(label, 0, 0, (int) totalCountForHour));
        }

        return new StaffDashboardDto(total, waiting, received, paid, recent, hourlyStats);
    }

    // 폼용 진료과 목록
    public List<StaffDepartmentOptionDto> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .filter(Department::isActive)
                .map(StaffDepartmentOptionDto::new)
                .collect(Collectors.toList());
    }

    // 폼용 의사 목록
    public List<StaffDoctorOptionDto> getAllDoctors() {
        return doctorRepository.findAllWithDetails().stream()
                .map(StaffDoctorOptionDto::new)
                .collect(Collectors.toList());
    }

}