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
        List<Reservation> active = doctorReservationRepository.findTodayActiveByDoctor(username, today, ReservationStatus.CANCELLED);
        List<Reservation> completed = doctorReservationRepository.findTodayByDoctorAndStatus(username, today, ReservationStatus.COMPLETED);

        int totalPatients = active.size();
        int completedCount = completed.size();
        int waitingCount = (int) active.stream()
                .filter(r -> r.getStatus() == ReservationStatus.RECEIVED)
                .count();

        List<DoctorReservationDto> schedulePreview = active.stream()
                .map(r -> {
                    long count = reservationRepository.countByPatient_IdAndStatus(r.getPatient().getId(), ReservationStatus.COMPLETED);
                    return new DoctorReservationDto(r, count == 0);
                })
                .toList();

        return new DoctorDashboardDto(totalPatients, completedCount, waitingCount, schedulePreview);
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

    public Page<DoctorReservationDto> getTreatmentPage(String username, int page) {
        LocalDate today = LocalDate.now();
        List<ReservationStatus> activeStatuses = List.of(ReservationStatus.RECEIVED, ReservationStatus.IN_TREATMENT);
        return doctorReservationRepository
                .findTodayByDoctorAndStatusesPage(username, today, activeStatuses, PageRequest.of(page, 10))
                .map(r -> {
                    long count = reservationRepository.countByPatient_IdAndStatus(r.getPatient().getId(), ReservationStatus.COMPLETED);
                    return new DoctorReservationDto(r, count == 0);
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
                    username, searchDate, ReservationStatus.COMPLETED, query, PageRequest.of(page, 10));
        } else {
            // 검색어가 없는 경우 (기존 방식)
            result = doctorReservationRepository.findTodayByDoctorAndStatusPage(
                    username, searchDate, ReservationStatus.COMPLETED, PageRequest.of(page, 10));
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
     * 특정 날짜의 전체 완료 환자 수를 조회합니다. (검색 필터 무시용)
     */
    public long getCompletedCountForDate(String username, LocalDate date) {
        LocalDate searchDate = (date != null) ? date : LocalDate.now();
        return doctorReservationRepository.findTodayByDoctorAndStatus(username, searchDate, ReservationStatus.COMPLETED).size();
    }

    // [W3-1] 폴링용: 오늘 날짜 RECEIVED + IN_TREATMENT 상태 조회
    public List<DoctorReservationDto> getTodayReceivedList(String username) {
        LocalDate today = LocalDate.now();
        List<ReservationStatus> activeStatuses = List.of(ReservationStatus.RECEIVED, ReservationStatus.IN_TREATMENT);
        return doctorReservationRepository
                .findTodayByDoctorAndStatuses(username, today, activeStatuses)
                .stream()
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
        List<PatientHistoryDto> history = reservationRepository
                .findByPatient_IdOrderByReservationDateDesc(reservation.getPatient().getId())
                .stream()
                .filter(r -> r.getStatus() == ReservationStatus.COMPLETED) // 진료 완료된 것만
                .filter(r -> !r.getId().equals(id)) // 현재 보고 있는 예약은 제외
                .map(r -> {
                    // 각 예약에 해당하는 진료 기록을 찾아서 DTO 생성
                    TreatmentRecord hRecord = treatmentRecordRepository.findByReservation_Id(r.getId()).orElse(null);
                    return new PatientHistoryDto(r, hRecord);
                })
                .toList();

        return new DoctorTreatmentDetailDto(reservation, record, count == 0, history);
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