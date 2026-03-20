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
            patientRepository.save(patient);
        } else if (!patient.getName().equals(request.getName())) {
            nameMismatch = true;
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

    // 환자 정보 수정
    @Transactional
    public void updatePatientInfo(PatientInfoUpdateRequest request) {
        Reservation reservation = reservationRepository.findById(request.getReservationId())
                .orElseThrow(() -> CustomException.notFound("예약을 찾을 수 없습니다."));
        Patient patient = reservation.getPatient();
        patient.updateAddressAndNote(request.getAddress(), request.getNote());
    }

    /**
     * 날짜별 예약 목록 조회
     */
    public List<StaffReservationDto> getReservations(LocalDate date, String status, String query, Long deptId, Long doctorId, String source) {
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
                        if (!matches) return false;
                    }
                    // 진료과 필터
                    if (deptId != null && !r.getDepartment().getId().equals(deptId)) return false;
                    // 전문의 필터
                    if (doctorId != null && !r.getDoctor().getId().equals(doctorId)) return false;
                    // 예약 구분 필터
                    if (source != null && !source.isBlank() && !r.getSource().name().equals(source)) return false;
                    
                    return true;
                })
                .map(r -> {
                    // 환자의 진료 완료 건수를 조회하여 초재진 여부를 판별합니다.
                    long completedCount = reservationRepository.countByPatient_IdAndStatus(r.getPatient().getId(), ReservationStatus.COMPLETED);
                    return new StaffReservationDto(r, completedCount);
                })
                .collect(Collectors.toList());
    }

    // 상태 필터 탭 목록
    public List<StaffStatusFilter> getStatusFilters(String selected, String date, String query, Long deptId, Long doctorId, String source) {
        String s = (selected == null) ? "" : selected;
        return List.of(
                new StaffStatusFilter("전체", "", s, date, query, deptId, doctorId, source),
                new StaffStatusFilter("접수 대기", "RESERVED", s, date, query, deptId, doctorId, source),
                new StaffStatusFilter("진료 대기", "RECEIVED", s, date, query, deptId, doctorId, source),
                new StaffStatusFilter("진료 완료", "COMPLETED", s, date, query, deptId, doctorId, source),
                new StaffStatusFilter("취소", "CANCELLED", s, date, query, deptId, doctorId, source));
    }

    /**
     * 예약 상세 조회
     */
    public StaffReservationDto getDetail(Long id) {
        Reservation r = reservationRepository.findByIdWithDetails(id)
                .orElseThrow(() -> CustomException.notFound("예약을 찾을 수 없습니다."));
        // 초재진 판별을 위한 진료 완료 건수 조회
        long completedCount = reservationRepository.countByPatient_IdAndStatus(r.getPatient().getId(), ReservationStatus.COMPLETED);
        return new StaffReservationDto(r, completedCount);
    }

    // 예약 취소
    @Transactional
    public Reservation cancel(Long id, String reason) {
        Reservation r = reservationRepository.findById(id)
                .orElseThrow(() -> CustomException.notFound("예약을 찾을 수 없습니다."));
        r.cancel(reason);
        return r;
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
        List<StaffReservationDto> recent = all.stream()
                .limit(5)
                .map(r -> {
                    // 환자의 초재진 판별을 위해 진료 완료 건수를 조회합니다.
                    long completedCount = reservationRepository.countByPatient_IdAndStatus(r.getPatient().getId(), ReservationStatus.COMPLETED);
                    return new StaffReservationDto(r, completedCount);
                })
                .collect(Collectors.toList());

        // 시간대별 통계 생성 (09시 ~ 18시)
        java.util.List<com.smartclinic.hms.staff.dto.StaffHourlyStatDto> hourlyStats = new java.util.ArrayList<>();
        for (int hour = 9; hour <= 18; hour++) {
            String label = String.format("%02d:00", hour);
            final int h = hour;

            long resCount = all.stream()
                    .filter(r -> r.getTimeSlot().startsWith(String.format("%02d:", h)))
                    .filter(r -> r.getStatus() == ReservationStatus.RESERVED)
                    .count();

            long recCount = all.stream()
                    .filter(r -> r.getTimeSlot().startsWith(String.format("%02d:", h)))
                    .filter(r -> r.getStatus() != ReservationStatus.RESERVED)
                    .count();

            int totalCount = (int) (resCount + recCount);
            hourlyStats.add(new com.smartclinic.hms.staff.dto.StaffHourlyStatDto(label, (int) resCount, (int) recCount, totalCount));
        }

        // 테스트용 샘플 데이터 (데이터가 없을 때도 그래프 확인용)
        if (all.isEmpty()) {
            hourlyStats.set(0, new com.smartclinic.hms.staff.dto.StaffHourlyStatDto("09:00", 2, 1, 3));
            hourlyStats.set(1, new com.smartclinic.hms.staff.dto.StaffHourlyStatDto("10:00", 5, 3, 8));
            hourlyStats.set(2, new com.smartclinic.hms.staff.dto.StaffHourlyStatDto("11:00", 3, 4, 7));
        }

        return new StaffDashboardDto(total, waiting, received, recent, hourlyStats);
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