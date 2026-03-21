package com.smartclinic.hms.nurse;

import com.smartclinic.hms.domain.*;
import com.smartclinic.hms.common.exception.CustomException;
import com.smartclinic.hms.doctor.DoctorRepository;
import com.smartclinic.hms.reservation.reservation.DepartmentRepository;
import com.smartclinic.hms.reservation.reservation.ReservationRepository;
import com.smartclinic.hms.staff.dto.StaffDepartmentOptionDto;
import com.smartclinic.hms.staff.dto.StaffDoctorOptionDto;
import com.smartclinic.hms.nurse.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import com.smartclinic.hms.doctor.treatment.DoctorTreatmentRecordRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NurseService {

    private final NursePatientStatusRepository nursePatientStatusRepository;
    private final ReservationRepository reservationRepository; // 초재진 확인용
    private final NursePatientRepository patientRepository;
    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorTreatmentRecordRepository treatmentRecordRepository;

    public NurseDashboardDto getDashboard() {
        LocalDate today = LocalDate.now();
        String todayDayOfWeek = toKoreanDayOfWeek(today.getDayOfWeek().getValue());

        // 1. 모든 의사 조회
        List<Doctor> allDoctors = doctorRepository.findAllWithDetails();
        
        // 2. 오늘 모든 예약(취소 제외) 조회
        List<Reservation> todayReservations = nursePatientStatusRepository.findTodayNonCancelled(today, ReservationStatus.CANCELLED);

        // 3. 의사별로 데이터 가공
        List<NurseDoctorStatusDto> doctorStatuses = allDoctors.stream()
                .map(doctor -> {
                    // 해당 의사의 오늘 예약들 필터링
                    List<Reservation> drRes = todayReservations.stream()
                            .filter(r -> r.getDoctor().getId().equals(doctor.getId()))
                            .toList();

                    // 오늘 진료 여부 확인 (예: "월,화,수"에 오늘 요일이 포함되는지)
                    boolean isAvailableToday = doctor.getAvailableDays() != null && doctor.getAvailableDays().contains(todayDayOfWeek);

                    // 상태별 집계
                    long totalToday = drRes.size();
                    long waitingCount = drRes.stream().filter(r -> r.getStatus() == ReservationStatus.RECEIVED).count();
                    long treatingCount = drRes.stream().filter(r -> r.getStatus() == ReservationStatus.IN_TREATMENT).count();
                    long pendingTreatmentCount = drRes.stream().filter(r -> r.getStatus() == ReservationStatus.COMPLETED && !r.isTreatmentCompleted()).count();
                    long paidCount = drRes.stream().filter(r -> r.isPaid()).count();

                    return new NurseDoctorStatusDto(
                            doctor.getStaff().getName(),
                            doctor.getDepartment().getName(),
                            isAvailableToday,
                            totalToday,
                            waitingCount,
                            treatingCount,
                            pendingTreatmentCount,
                            paidCount
                    );
                })
                .toList();

        return new NurseDashboardDto(doctorStatuses);
    }

    /**
     * Java 요일 숫자(1:월 ~ 7:일)를 한글 요일명("월", "화" 등)으로 변환
     */
    private String toKoreanDayOfWeek(int dayValue) {
        return switch (dayValue) {
            case 1 -> "월";
            case 2 -> "화";
            case 3 -> "수";
            case 4 -> "목";
            case 5 -> "금";
            case 6 -> "토";
            case 7 -> "일";
            default -> "";
        };
    }

    public Page<NursePatientStatusDto> getReceptionPage(String status, String query, Long deptId, Long doctorId,
            String source, int page) {
        List<Long> deptIds = deptId != null ? List.of(deptId) : null;
        List<Long> doctorIds = doctorId != null ? List.of(doctorId) : null;
        return getReceptionPageWithMultiFilters(status, query, deptIds, doctorIds, source, page);
    }

    /**
     * 다중 선택 필터를 지원하는 환자 현황 페이지 조회
     */
    public Page<NursePatientStatusDto> getReceptionPageWithMultiFilters(String status, String query, 
            List<Long> deptIds, List<Long> doctorIds, String source, int page) {
        LocalDate today = LocalDate.now();
        PageRequest pageable = PageRequest.of(page, 10);

        // 빈 리스트인 경우 쿼리 조건(IS NULL)이 정상 동작하도록 null 처리
        List<Long> effectiveDeptIds = (deptIds == null || deptIds.isEmpty()) ? null : deptIds;
        List<Long> effectiveDoctorIds = (doctorIds == null || doctorIds.isEmpty()) ? null : doctorIds;

        ReservationSource src = null;
        if (source != null && !source.isBlank()) {
            try {
                src = ReservationSource.valueOf(source);
            } catch (Exception ignored) {
            }
        }

        Page<Reservation> reservationPage;
        if (status == null || status.isBlank()) {
            reservationPage = nursePatientStatusRepository
                    .findTodayNonCancelledWithMultiFiltersPage(today, ReservationStatus.CANCELLED, query, effectiveDeptIds, effectiveDoctorIds,
                            src, pageable);
        } else {
            try {
                ReservationStatus st = ReservationStatus.valueOf(status);
                reservationPage = nursePatientStatusRepository
                        .findTodayByStatusWithMultiFiltersPage(today, st, query, effectiveDeptIds, effectiveDoctorIds, src, pageable);
            } catch (IllegalArgumentException e) {
                reservationPage = nursePatientStatusRepository
                        .findTodayNonCancelledWithMultiFiltersPage(today, ReservationStatus.CANCELLED, query, effectiveDeptIds, effectiveDoctorIds,
                                src, pageable);
            }
        }

        return reservationPage.map(r -> {
            long count = reservationRepository.countByPatient_IdAndStatus(r.getPatient().getId(), ReservationStatus.COMPLETED);
            return new NursePatientStatusDto(r, count == 0);
        });
    }

    public List<NurseStatusFilter> getStatusFilters(String selected, String query, Long deptId, Long doctorId,
            String source) {
        String sel = selected == null ? "" : selected;

        StringBuilder commonParams = new StringBuilder();
        if (query != null && !query.isBlank())
            commonParams.append("&query=").append(query);
        if (deptId != null)
            commonParams.append("&deptId=").append(deptId);
        if (doctorId != null)
            commonParams.append("&doctorId=").append(doctorId);
        if (source != null && !source.isBlank())
            commonParams.append("&source=").append(source);

        String common = commonParams.toString();

        return List.of(
                new NurseStatusFilter("전체", "", sel.isEmpty(),
                        "/nurse/reception-list" + (common.isEmpty() ? "" : "?" + common.substring(1))),
                new NurseStatusFilter("예약됨", "RESERVED", "RESERVED".equals(sel),
                        "/nurse/reception-list?status=RESERVED" + common),
                new NurseStatusFilter("진료 대기", "RECEIVED", "RECEIVED".equals(sel),
                        "/nurse/reception-list?status=RECEIVED" + common),
                new NurseStatusFilter("진료 완료", "COMPLETED", "COMPLETED".equals(sel),
                        "/nurse/reception-list?status=COMPLETED" + common));
    }

    public List<StaffDepartmentOptionDto> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .filter(Department::isActive)
                .map(StaffDepartmentOptionDto::new)
                .collect(Collectors.toList());
    }

    public List<StaffDoctorOptionDto> getAllDoctors() {
        return doctorRepository.findAllWithDetails().stream()
                .map(StaffDoctorOptionDto::new)
                .collect(Collectors.toList());
    }

    public NursePatientDto getPatientDetail(Long reservationId) {
        Reservation r = nursePatientStatusRepository.findByIdWithDetails(reservationId)
                .orElseThrow(() -> CustomException.notFound("환자 상세 정보를 찾을 수 없습니다."));
        
        long completedCount = reservationRepository.countByPatient_IdAndStatus(r.getPatient().getId(), ReservationStatus.COMPLETED);
        NursePatientDto dto = new NursePatientDto(r, completedCount == 0);

        // 1. 현재 진료 기록 조회
        treatmentRecordRepository.findByReservation_Id(reservationId).ifPresent(tr -> {
            dto.setDiagnosis(tr.getDiagnosis());
            dto.setPrescription(tr.getPrescription());
            dto.setRemark(tr.getRemark());
            dto.setNurseNote(tr.getNurseNote());
        });

        // 2. 과거 히스토리 조회 (현재 건 제외 모든 완료된 예약)
        List<Reservation> histories = reservationRepository.findByPatient_IdOrderByReservationDateDesc(
                r.getPatient().getId());
        
        List<NursePatientDto.NurseTreatmentHistoryDto> historyDtos = histories.stream()
            .filter(h -> !h.getId().equals(reservationId) && h.getStatus() == ReservationStatus.COMPLETED)
            .map(h -> {
                TreatmentRecord tr = treatmentRecordRepository.findByReservation_Id(h.getId()).orElse(null);
                return new NursePatientDto.NurseTreatmentHistoryDto(
                    h.getReservationDate().toString(),
                    h.getDepartment().getId(),
                    h.getDoctor().getId(),
                    h.getDoctor().getStaff().getName(),
                    h.getDepartment().getName(),
                    tr != null ? tr.getDiagnosis() : "",
                    tr != null ? tr.getPrescription() : "",
                    tr != null ? tr.getRemark() : "",
                    tr != null ? tr.getNurseNote() : ""
                );
            })
            .toList();
        
        dto.setHistory(historyDtos);
        return dto;
    }

    /**
     * 간호 메모 저장
     */
    @Transactional
    public void saveNurseNote(Long reservationId, String nurseNote) {
        TreatmentRecord tr = treatmentRecordRepository.findByReservation_Id(reservationId)
                .orElseThrow(() -> CustomException.notFound("진료 기록이 존재하지 않습니다. 의사 진료가 먼저 완료되어야 합니다."));
        tr.updateNurseNote(nurseNote);
    }

    @Transactional
    public void receiveReservation(Long reservationId) {
        Reservation r = nursePatientStatusRepository.findById(reservationId)
                .orElseThrow(() -> CustomException.notFound("환자 현황 정보를 찾을 수 없습니다."));
        r.receive();
    }

    /**
     * 간호사 처치 완료 처리
     */
    @Transactional
    public void completeTreatment(Long reservationId) {
        Reservation r = nursePatientStatusRepository.findById(reservationId)
                .orElseThrow(() -> CustomException.notFound("환자 정보를 찾을 수 없습니다."));
        r.completeTreatment();
    }

    @Transactional
    public void updatePatient(Long patientId, String phone, String address, String note) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> CustomException.notFound("환자 정보를 찾을 수 없습니다."));
        patient.updateInfo(patient.getName(), phone, patient.getEmail(), address, note);
    }
}