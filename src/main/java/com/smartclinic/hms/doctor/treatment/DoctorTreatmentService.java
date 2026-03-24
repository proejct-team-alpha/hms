package com.smartclinic.hms.doctor.treatment;

import com.smartclinic.hms.common.exception.CustomException;
import com.smartclinic.hms.doctor.DoctorRepository;
import com.smartclinic.hms.doctor.treatment.dto.DoctorDashboardDto;
import com.smartclinic.hms.doctor.treatment.dto.DoctorReservationDto;
import com.smartclinic.hms.doctor.treatment.dto.DoctorTreatmentDetailDto;
import com.smartclinic.hms.domain.Doctor;
import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationStatus;
import com.smartclinic.hms.domain.TreatmentRecord;
import com.smartclinic.hms.domain.PatientHistoryDto;
import com.smartclinic.hms.reservation.reservation.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorTreatmentService {

    private final DoctorReservationRepository doctorReservationRepository;
    private final ReservationRepository reservationRepository; // 초재진 확인용
    private final DoctorTreatmentRecordRepository treatmentRecordRepository;
    private final DoctorRepository doctorRepository;

    public DoctorDashboardDto getDashboardData(String username) {
        LocalDate today = LocalDate.now();
        // 1. 오늘 전체 데이터 조회 (CANCELLED 제외)
        List<Reservation> allToday = doctorReservationRepository.findTodayActiveByDoctor(username, today, ReservationStatus.CANCELLED);

        // 2. 각 상태별 카운트 계산
        int waitingCount = (int) allToday.stream()
                .filter(r -> r.getStatus() == ReservationStatus.RECEIVED || r.getStatus() == ReservationStatus.IN_TREATMENT)
                .count();
        int completedCount = (int) allToday.stream()
                .filter(r -> r.getStatus() == ReservationStatus.COMPLETED)
                .count();
        int reservedCount = (int) allToday.stream()
                .filter(r -> r.getStatus() == ReservationStatus.RESERVED)
                .count();
        
        // 의사님 정의: 오늘의 환자 수 = 대기 + 완료
        int totalToday = waitingCount + completedCount;

        // [수정] 업무 부하 스타일 결정 로직 (명시적 클래스 할당)
        String boxClass, iconClass, iconName, btnClass;
        if (waitingCount >= 6) {
            boxClass = "bg-red-50 border-red-500 text-red-900";
            iconClass = "bg-red-100 text-red-600";
            iconName = "alert-triangle";
            btnClass = "bg-red-600 hover:bg-red-700";
        } else if (waitingCount >= 3) {
            boxClass = "bg-orange-50 border-orange-500 text-orange-900";
            iconClass = "bg-orange-100 text-orange-600";
            iconName = "zap";
            btnClass = "bg-orange-600 hover:bg-orange-700";
        } else if (waitingCount >= 1) {
            boxClass = "bg-indigo-50 border-indigo-500 text-indigo-900";
            iconClass = "bg-indigo-100 text-indigo-600";
            iconName = "bell";
            btnClass = "bg-indigo-600 hover:bg-indigo-700";
        } else {
            boxClass = "bg-slate-50 border-slate-400 text-slate-600";
            iconClass = "bg-slate-200 text-slate-500";
            iconName = "check";
            btnClass = "bg-slate-600 hover:bg-slate-700";
        }

        // 3. 스케줄 미리보기 리스트 생성 및 정렬 (진료중 > 대기 > 예약 > 완료 순)
        List<DoctorReservationDto> schedulePreview = allToday.stream()
                .sorted((r1, r2) -> {
                    int p1 = getStatusPriority(r1.getStatus());
                    int p2 = getStatusPriority(r2.getStatus());
                    if (p1 != p2) return p1 - p2;
                    return r1.getTimeSlot().compareTo(r2.getTimeSlot());
                })
                .limit(12) // 대시보드 가독성을 위해 상위 12명으로 제한
                .map(r -> {
                    long count = reservationRepository.countByPatient_IdAndStatus(r.getPatient().getId(), ReservationStatus.COMPLETED);
                    return new DoctorReservationDto(r, count == 0);
                })
                .toList();

        return new DoctorDashboardDto(totalToday, waitingCount, completedCount, reservedCount, 
                                     boxClass, iconClass, iconName, btnClass, schedulePreview);
    }

    private int getStatusPriority(ReservationStatus status) {
        return switch (status) {
            case IN_TREATMENT -> 1;
            case RECEIVED -> 2;
            case RESERVED -> 3;
            case COMPLETED -> 4;
            default -> 5;
        };
    }

    public List<DoctorReservationDto> getTreatmentList(String username) {
        LocalDate today = LocalDate.now();
        return doctorReservationRepository
                .findTodayActiveByDoctor(username, today, ReservationStatus.CANCELLED)
                .stream()
                .map(r -> {
                    long count = reservationRepository.countByPatient_IdAndStatus(r.getPatient().getId(), ReservationStatus.COMPLETED);
                    return new DoctorReservationDto(r, count == 0);
                })
                .toList();
    }

    public Page<DoctorReservationDto> getTreatmentPage(String username, LocalDate date, String tab, String query, int page) {
        LocalDate today = LocalDate.now();
        List<ReservationStatus> statuses;
        int size = 20;

        // [기능 추가] 대기/진료중 탭(waiting)인 경우 페이징 없이 모든 환자(최대 1000명)를 한 화면에 노출
        if ("waiting".equals(tab) || tab == null || tab.isEmpty()) {
            size = 1000;
            page = 0; // 대기 목록은 항상 전체를 보여주기 위해 0페이지로 고정
        }

        // 과거 날짜인 경우 모든 상태 조회, 오늘인 경우 탭에 따라 필터링
        if (date.isBefore(today)) {
            statuses = List.of(ReservationStatus.RECEIVED, ReservationStatus.IN_TREATMENT, ReservationStatus.COMPLETED);
        } else {
            if ("completed".equals(tab)) {
                statuses = List.of(ReservationStatus.COMPLETED);
            } else if ("all".equals(tab)) {
                statuses = List.of(ReservationStatus.RECEIVED, ReservationStatus.IN_TREATMENT, ReservationStatus.COMPLETED);
            } else {
                statuses = List.of(ReservationStatus.RECEIVED, ReservationStatus.IN_TREATMENT);
            }
        }

        Page<Reservation> resultPage;
        if (query != null && !query.isBlank()) {
            resultPage = doctorReservationRepository.findTodayByDoctorAndStatusesAndPatientNamePage(
                    username, date, statuses, query, PageRequest.of(page, size));
        } else {
            resultPage = doctorReservationRepository.findTodayByDoctorAndStatusesPage(
                    username, date, statuses, PageRequest.of(page, size));
        }

        java.util.concurrent.atomic.AtomicInteger index = new java.util.concurrent.atomic.AtomicInteger(page * size + 1);
        return resultPage.map(r -> {
                    long count = reservationRepository.countByPatient_IdAndStatus(r.getPatient().getId(), ReservationStatus.COMPLETED);
                    // 확정된 진단명 가져오기 (진료 완료된 경우)
                    String diagnosis = null;
                    if (r.getStatus() == ReservationStatus.COMPLETED) {
                        diagnosis = treatmentRecordRepository.findByReservation_Id(r.getId())
                                .map(com.smartclinic.hms.domain.TreatmentRecord::getDiagnosis)
                                .orElse("기록 없음");
                    }
                    DoctorReservationDto dto = new DoctorReservationDto(r, count == 0, diagnosis);
                    dto.setSequence(index.getAndIncrement());
                    return dto;
                });
    }

    public List<DoctorReservationDto> getCompletedList(String username, LocalDate date) {
        LocalDate searchDate = (date != null) ? date : LocalDate.now();
        return doctorReservationRepository
                .findTodayByDoctorAndStatus(username, searchDate, ReservationStatus.COMPLETED)
                .stream()
                .map(r -> {
                    long count = reservationRepository.countByPatient_IdAndStatus(r.getPatient().getId(), ReservationStatus.COMPLETED);
                    // 확정된 진단명 가져오기
                    String diagnosis = treatmentRecordRepository.findByReservation_Id(r.getId())
                            .map(com.smartclinic.hms.domain.TreatmentRecord::getDiagnosis)
                            .orElse("기록 없음");
                    return new DoctorReservationDto(r, count == 0, diagnosis);
                })
                .toList();
    }

    public Page<DoctorReservationDto> getCompletedPage(String username, LocalDate date, String query, int page) {
        LocalDate searchDate = (date != null) ? date : LocalDate.now();
        Page<Reservation> result;
        
        if (query != null && !query.isBlank()) {
            // 이름 검색어가 있는 경우
            result = doctorReservationRepository.findTodayByDoctorAndStatusAndPatientNamePage(
                    username, searchDate, ReservationStatus.COMPLETED, query, PageRequest.of(page, 20));
        } else {
            // 검색어가 없는 경우 (기존 방식)
            result = doctorReservationRepository.findTodayByDoctorAndStatusPage(
                    username, searchDate, ReservationStatus.COMPLETED, PageRequest.of(page, 20));
        }

        return result.map(r -> {
            long count = reservationRepository.countByPatient_IdAndStatus(r.getPatient().getId(), ReservationStatus.COMPLETED);
            // 확정된 진단명 가져오기
            String diagnosis = treatmentRecordRepository.findByReservation_Id(r.getId())
                    .map(com.smartclinic.hms.domain.TreatmentRecord::getDiagnosis)
                    .orElse("기록 없음");
            return new DoctorReservationDto(r, count == 0, diagnosis);
        });
    }

    /**
     * 특정 날짜의 전체 환자 수(대기 + 진료중 + 완료)를 조회합니다.
     */
    public long getDailyTotalCount(String username, LocalDate date) {
        List<ReservationStatus> activeStatuses = List.of(
                ReservationStatus.RECEIVED, 
                ReservationStatus.IN_TREATMENT, 
                ReservationStatus.COMPLETED
        );
        return doctorReservationRepository.findTodayByDoctorAndStatuses(username, date, activeStatuses).size();
    }

    /**
     * 특정 날짜의 전체 완료 환자 수를 조회합니다. (검색 필터 무시용)
     */
    public long getCompletedCountForDate(String username, LocalDate date) {
        LocalDate searchDate = (date != null) ? date : LocalDate.now();
        return doctorReservationRepository.findTodayByDoctorAndStatus(username, searchDate, ReservationStatus.COMPLETED).size();
    }

    // [W3-1] 폴링용: 오늘 날짜 RECEIVED + IN_TREATMENT 상태 조회 (검색 지원)
    public List<DoctorReservationDto> getTodayReceivedList(String username, String query) {
        LocalDate today = LocalDate.now();
        List<ReservationStatus> activeStatuses = List.of(ReservationStatus.RECEIVED, ReservationStatus.IN_TREATMENT);
        
        List<Reservation> list;
        if (query != null && !query.isBlank()) {
            list = doctorReservationRepository.findTodayByDoctorAndStatusesAndPatientName(username, today, activeStatuses, query);
        } else {
            list = doctorReservationRepository.findTodayByDoctorAndStatuses(username, today, activeStatuses);
        }

        return list.stream()
                .map(r -> {
                    long count = reservationRepository.countByPatient_IdAndStatus(r.getPatient().getId(), ReservationStatus.COMPLETED);
                    return new DoctorReservationDto(r, count == 0);
                })
                .toList();
    }

    @Transactional
    public void startTreatment(Long id, String username) {
        Reservation reservation = doctorReservationRepository.findByIdAndDoctor(id, username)
                .orElseThrow(() -> CustomException.notFound("예약을 찾을 수 없습니다."));
        reservation.startTreatment();
    }

    public DoctorTreatmentDetailDto getTreatmentDetail(Long id, String username) {
        Reservation reservation = doctorReservationRepository.findByIdAndDoctor(id, username)
                .orElseThrow(() -> CustomException.notFound("예약을 찾을 수 없습니다."));
        TreatmentRecord record = treatmentRecordRepository.findByReservation_Id(id).orElse(null);
        
        // 1. 초재진 판별을 위한 카운트
        long count = reservationRepository.countByPatient_IdAndStatus(reservation.getPatient().getId(), ReservationStatus.COMPLETED);
        
        // 2. 과거 진료 히스토리 조회 (상태가 COMPLETED인 것만 필터링)
        List<Reservation> pastReservations = reservationRepository
                .findByPatient_IdOrderByReservationDateDesc(reservation.getPatient().getId())
                .stream()
                .filter(r -> r.getStatus() == ReservationStatus.COMPLETED) // 진료 완료된 것만
                .filter(r -> !r.getId().equals(id)) // 현재 보고 있는 예약은 제외
                .toList();

        List<com.smartclinic.hms.domain.PatientHistoryDto> history = pastReservations.stream()
                .map(r -> {
                    // 각 예약에 해당하는 진료 기록을 찾아서 DTO 생성
                    TreatmentRecord hRecord = treatmentRecordRepository.findByReservation_Id(r.getId()).orElse(null);
                    return new com.smartclinic.hms.domain.PatientHistoryDto(r, hRecord);
                })
                .toList();

        // [추가] 3. 필터링을 위한 고유 진료과 및 의사 목록 생성 (과거 이력 기반)
        List<com.smartclinic.hms.staff.dto.StaffDepartmentOptionDto> filterDepartments = pastReservations.stream()
                .map(Reservation::getDepartment)
                .distinct()
                .map(com.smartclinic.hms.staff.dto.StaffDepartmentOptionDto::new)
                .toList();

        List<com.smartclinic.hms.staff.dto.StaffDoctorOptionDto> filterDoctors = pastReservations.stream()
                .map(Reservation::getDoctor)
                .distinct()
                .map(com.smartclinic.hms.staff.dto.StaffDoctorOptionDto::new)
                .toList();

        return new DoctorTreatmentDetailDto(reservation, record, count == 0, history, filterDepartments, filterDoctors);
    }

    @Transactional
    public void saveTreatmentRecord(Long id, String username, String diagnosis, String prescription, String remark) {
        Reservation reservation = doctorReservationRepository.findByIdAndDoctor(id, username)
                .orElseThrow(() -> CustomException.notFound("예약을 찾을 수 없습니다."));
        Doctor doctor = doctorRepository.findByStaff_Username(username)
                .orElseThrow(() -> CustomException.notFound("의사 정보를 찾을 수 없습니다."));

        TreatmentRecord record = treatmentRecordRepository.findByReservation_Id(id).orElse(null);
        if (record != null) {
            record.update(diagnosis, prescription, remark);
        } else {
            record = TreatmentRecord.create(reservation, doctor, diagnosis, prescription, remark);
            treatmentRecordRepository.save(record);
        }
    }

    @Transactional
    public void completeTreatment(Long id, String username, String diagnosis, String prescription, String remark) {
        Reservation reservation = doctorReservationRepository.findByIdAndDoctor(id, username)
                .orElseThrow(() -> CustomException.notFound("예약을 찾을 수 없습니다."));
        Doctor doctor = doctorRepository.findByStaff_Username(username)
                .orElseThrow(() -> CustomException.notFound("의사 정보를 찾을 수 없습니다."));

        reservation.complete();

        TreatmentRecord record = treatmentRecordRepository.findByReservation_Id(id).orElse(null);
        if (record != null) {
            record.update(diagnosis, prescription, remark);
        } else {
            record = TreatmentRecord.create(reservation, doctor, diagnosis, prescription, remark);
            treatmentRecordRepository.save(record);
        }
    }
}