package com.smartclinic.hms.doctor.treatment;

import com.smartclinic.hms.doctor.DoctorRepository;
import com.smartclinic.hms.doctor.treatment.dto.DoctorDashboardDto;
import com.smartclinic.hms.doctor.treatment.dto.DoctorReservationDto;
import com.smartclinic.hms.doctor.treatment.dto.DoctorTreatmentDetailDto;
import com.smartclinic.hms.domain.Doctor;
import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationStatus;
import com.smartclinic.hms.domain.TreatmentRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorTreatmentService {

    private final DoctorReservationRepository reservationRepository;
    private final DoctorTreatmentRecordRepository treatmentRecordRepository;
    private final DoctorRepository doctorRepository;

    @Transactional(readOnly = true)
    public DoctorDashboardDto getDashboardData(String username) {
        LocalDate today = LocalDate.now();
        List<Reservation> active = reservationRepository.findTodayActiveByDoctor(username, today, ReservationStatus.CANCELLED);
        List<Reservation> completed = reservationRepository.findTodayByDoctorAndStatus(username, today, ReservationStatus.COMPLETED);

        int totalPatients = active.size() + completed.size();
        int completedCount = completed.size();
        int waitingCount = (int) active.stream()
                .filter(r -> r.getStatus() == ReservationStatus.RECEIVED)
                .count();

        List<DoctorReservationDto> schedulePreview = active.stream()
                .map(DoctorReservationDto::new)
                .toList();

        return new DoctorDashboardDto(totalPatients, completedCount, waitingCount, schedulePreview);
    }

    @Transactional(readOnly = true)
    public List<DoctorReservationDto> getTreatmentList(String username) {
        LocalDate today = LocalDate.now();
        return reservationRepository
                .findTodayActiveByDoctor(username, today, ReservationStatus.CANCELLED)
                .stream()
                .map(DoctorReservationDto::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DoctorReservationDto> getCompletedList(String username) {
        LocalDate today = LocalDate.now();
        return reservationRepository
                .findTodayByDoctorAndStatus(username, today, ReservationStatus.COMPLETED)
                .stream()
                .map(DoctorReservationDto::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public DoctorTreatmentDetailDto getTreatmentDetail(Long id, String username) {
        Reservation reservation = reservationRepository.findByIdAndDoctor(id, username)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));
        TreatmentRecord record = treatmentRecordRepository.findByReservation_Id(id).orElse(null);
        return new DoctorTreatmentDetailDto(reservation, record);
    }

    @Transactional
    public void completeTreatment(Long id, String username, String diagnosis, String prescription, String remark) {
        Reservation reservation = reservationRepository.findByIdAndDoctor(id, username)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));
        Doctor doctor = doctorRepository.findByStaff_Username(username)
                .orElseThrow(() -> new IllegalArgumentException("의사 정보를 찾을 수 없습니다."));

        reservation.complete();

        TreatmentRecord record = TreatmentRecord.create(
                reservation, doctor, diagnosis, prescription, remark != null ? remark : "");
        treatmentRecordRepository.save(record);
    }
}
