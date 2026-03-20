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
                .findTodayByDoctorAndStatusesPage(username, today, activeStatuses, PageRequest.of(page, 9))
                .map(r -> {
                    long count = reservationRepository.countByPatient_IdAndStatus(r.getPatient().getId(), ReservationStatus.COMPLETED);
                    return new DoctorReservationDto(r, count == 0);
                });
    }

    public List<DoctorReservationDto> getCompletedList(String username) {
        LocalDate today = LocalDate.now();
        return doctorReservationRepository
                .findTodayByDoctorAndStatus(username, today, ReservationStatus.COMPLETED)
                .stream()
                .map(r -> {
                    long count = reservationRepository.countByPatient_IdAndStatus(r.getPatient().getId(), ReservationStatus.COMPLETED);
                    return new DoctorReservationDto(r, count == 0);
                })
                .toList();
    }

    public Page<DoctorReservationDto> getCompletedPage(String username, int page) {
        LocalDate today = LocalDate.now();
        return doctorReservationRepository
                .findTodayByDoctorAndStatusPage(username, today, ReservationStatus.COMPLETED, PageRequest.of(page, 9))
                .map(r -> {
                    long count = reservationRepository.countByPatient_IdAndStatus(r.getPatient().getId(), ReservationStatus.COMPLETED);
                    return new DoctorReservationDto(r, count == 0);
                });
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
        long count = reservationRepository.countByPatient_IdAndStatus(reservation.getPatient().getId(), ReservationStatus.COMPLETED);
        return new DoctorTreatmentDetailDto(reservation, record, count == 0);
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