package com.smartclinic.hms.nurse;

import com.smartclinic.hms.domain.Patient;
import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationStatus;
import com.smartclinic.hms.nurse.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NurseService {

    private final NurseReservationRepository reservationRepository;
    private final NursePatientRepository patientRepository;

    @Transactional(readOnly = true)
    public NurseDashboardDto getDashboard() {
        LocalDate today = LocalDate.now();
        List<Reservation> all = reservationRepository.findTodayNonCancelled(today, ReservationStatus.CANCELLED);

        List<NurseReservationDto> waitingList = all.stream()
                .filter(r -> r.getStatus() == ReservationStatus.RECEIVED)
                .map(NurseReservationDto::new)
                .toList();

        long specialCount = all.stream()
                .filter(r -> r.getPatient().getNote() != null && !r.getPatient().getNote().isBlank())
                .count();

        return new NurseDashboardDto(all.size(), waitingList.size(), specialCount, waitingList);
    }

    @Transactional(readOnly = true)
    public List<NurseReservationDto> getReceptionList(String status) {
        LocalDate today = LocalDate.now();
        if (status == null || status.isBlank()) {
            return reservationRepository.findTodayNonCancelled(today, ReservationStatus.CANCELLED)
                    .stream().map(NurseReservationDto::new).toList();
        }
        try {
            ReservationStatus st = ReservationStatus.valueOf(status);
            return reservationRepository.findTodayByStatus(today, st)
                    .stream().map(NurseReservationDto::new).toList();
        } catch (IllegalArgumentException e) {
            return reservationRepository.findTodayNonCancelled(today, ReservationStatus.CANCELLED)
                    .stream().map(NurseReservationDto::new).toList();
        }
    }

    public List<NurseStatusFilter> getStatusFilters(String selected) {
        String sel = selected == null ? "" : selected;
        return List.of(
                new NurseStatusFilter("전체", "", sel.isEmpty(), "/nurse/reception-list"),
                new NurseStatusFilter("예약됨", "RESERVED", "RESERVED".equals(sel), "/nurse/reception-list?status=RESERVED"),
                new NurseStatusFilter("진료 대기", "RECEIVED", "RECEIVED".equals(sel), "/nurse/reception-list?status=RECEIVED"),
                new NurseStatusFilter("진료 완료", "COMPLETED", "COMPLETED".equals(sel), "/nurse/reception-list?status=COMPLETED")
        );
    }

    @Transactional(readOnly = true)
    public NursePatientDto getPatientDetail(Long reservationId) {
        Reservation r = reservationRepository.findByIdWithDetails(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("예약 정보를 찾을 수 없습니다."));
        return new NursePatientDto(r);
    }

    @Transactional
    public void receiveReservation(Long reservationId) {
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("예약 정보를 찾을 수 없습니다."));
        r.receive();
    }

    @Transactional
    public void updatePatient(Long patientId, String phone, String address, String note) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("환자 정보를 찾을 수 없습니다."));
        patient.updateInfo(patient.getName(), phone, patient.getEmail(), address, note);
    }
}
