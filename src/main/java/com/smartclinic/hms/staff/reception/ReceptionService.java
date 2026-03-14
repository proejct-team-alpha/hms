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
import com.smartclinic.hms.staff.reception.dto.ReceptionUpdateRequest;
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

    // 전화 예약 생성
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

    // 접수 목록 조회
    @Transactional
    public List<Reservation> getReservations() {

        List<Reservation> reservations = reservationRepository.findAll();

        // LAZY 로딩 방지
        for (Reservation r : reservations) {
            r.getPatient().getName();
            r.getDoctor().getStaff().getName();
            r.getDepartment().getName();
        }

        return reservations;
    }

    // 접수 처리
    @Transactional
    public void receive(ReceptionUpdateRequest request) {

        Reservation reservation = reservationRepository.findById(request.getReservationId())
                .orElseThrow(() -> new RuntimeException("예약 없음"));
        reservation.receive();
    }

    // 오늘 예약 목록 (취소 제외 or 특정 상태)
    public List<StaffReservationDto> getTodayReservations(String status) {
        LocalDate today = LocalDate.now();
        List<Reservation> reservations;
        if (status == null || status.isBlank()) {
            reservations = reservationRepository.findTodayExcludingStatus(today, ReservationStatus.CANCELLED);
        } else {
            reservations = reservationRepository.findTodayByStatus(today, ReservationStatus.valueOf(status));
        }
        return reservations.stream().map(StaffReservationDto::new).collect(Collectors.toList());
    }

    // 상태 필터 탭 목록
    public List<StaffStatusFilter> getStatusFilters(String selected) {
        String s = (selected == null) ? "" : selected;
        return List.of(
                new StaffStatusFilter("전체", "", s),
                new StaffStatusFilter("접수 대기", "RESERVED", s),
                new StaffStatusFilter("진료 대기", "RECEIVED", s),
                new StaffStatusFilter("진료 완료", "COMPLETED", s));
    }

    // 예약 상세 조회
    public StaffReservationDto getDetail(Long id) {
        Reservation r = reservationRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("예약 없음"));
        return new StaffReservationDto(r);
    }

    // 예약 취소
    @Transactional
    public void cancel(Long id) {
        Reservation r = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("예약 없음"));
        r.cancel();
    }

    // 대시보드 통계
    public StaffDashboardDto getDashboard() {
        LocalDate today = LocalDate.now();
        List<Reservation> all = reservationRepository.findTodayExcludingStatus(today, ReservationStatus.CANCELLED);
        int total    = all.size();
        int waiting  = (int) all.stream().filter(r -> r.getStatus() == ReservationStatus.RESERVED).count();
        int received = (int) all.stream().filter(r -> r.getStatus() == ReservationStatus.RECEIVED).count();
        List<StaffReservationDto> recent = all.stream()
                .limit(5)
                .map(StaffReservationDto::new)
                .collect(Collectors.toList());
        return new StaffDashboardDto(total, waiting, received, recent);
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