package com.smartclinic.hms.nurse;

import com.smartclinic.hms.common.exception.CustomException;
import com.smartclinic.hms.domain.Patient;
import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationStatus;
import com.smartclinic.hms.nurse.dto.*;
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
public class NurseService {

    private final NurseReservationRepository reservationRepository;
    private final NursePatientRepository patientRepository;

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

    public Page<NurseReservationDto> getReceptionPage(String status, int page) {
        LocalDate today = LocalDate.now();
        PageRequest pageable = PageRequest.of(page, 10);
        if (status == null || status.isBlank()) {
            return reservationRepository.findTodayNonCancelledPage(today, ReservationStatus.CANCELLED, pageable)
                    .map(NurseReservationDto::new);
        }
        try {
            ReservationStatus st = ReservationStatus.valueOf(status);
            return reservationRepository.findTodayByStatusPage(today, st, pageable)
                    .map(NurseReservationDto::new);
        } catch (IllegalArgumentException e) {
            return reservationRepository.findTodayNonCancelledPage(today, ReservationStatus.CANCELLED, pageable)
                    .map(NurseReservationDto::new);
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

    public NursePatientDto getPatientDetail(Long reservationId) {
        Reservation r = reservationRepository.findByIdWithDetails(reservationId)
                .orElseThrow(() -> CustomException.notFound("예약 정보를 찾을 수 없습니다."));
        return new NursePatientDto(r);
    }

    @Transactional
    public void receiveReservation(Long reservationId) {
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> CustomException.notFound("예약 정보를 찾을 수 없습니다."));
        r.receive();
    }

    @Transactional
    public void updatePatient(Long patientId, String phone, String address, String note) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> CustomException.notFound("환자 정보를 찾을 수 없습니다."));
        patient.updateInfo(patient.getName(), phone, patient.getEmail(), address, note);
    }
}
