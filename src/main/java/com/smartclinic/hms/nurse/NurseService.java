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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NurseService {

    private final NurseReservationRepository nurseReservationRepository;
    private final ReservationRepository reservationRepository; // 초재진 확인용
    private final NursePatientRepository patientRepository;
    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;

    public NurseDashboardDto getDashboard() {
        LocalDate today = LocalDate.now();
        List<Reservation> all = nurseReservationRepository.findTodayNonCancelled(today, ReservationStatus.CANCELLED);

        List<NurseReservationDto> waitingList = all.stream()
                .filter(r -> r.getStatus() == ReservationStatus.RECEIVED)
                .map(r -> {
                    long count = reservationRepository.countByPatient_IdAndStatus(r.getPatient().getId(), ReservationStatus.COMPLETED);
                    return new NurseReservationDto(r, count == 0);
                })
                .toList();

        long specialCount = all.stream()
                .filter(r -> r.getPatient().getNote() != null && !r.getPatient().getNote().isBlank())
                .count();

        return new NurseDashboardDto(all.size(), waitingList.size(), specialCount, waitingList);
    }

    public Page<NurseReservationDto> getReceptionPage(String status, String query, Long deptId, Long doctorId,
            String source, int page) {
        LocalDate today = LocalDate.now();
        PageRequest pageable = PageRequest.of(page, 10);

        ReservationSource src = null;
        if (source != null && !source.isBlank()) {
            try {
                src = ReservationSource.valueOf(source);
            } catch (Exception ignored) {
            }
        }

        Page<Reservation> reservationPage;
        if (status == null || status.isBlank()) {
            reservationPage = nurseReservationRepository
                    .findTodayNonCancelledWithFiltersPage(today, ReservationStatus.CANCELLED, query, deptId, doctorId,
                            src, pageable);
        } else {
            try {
                ReservationStatus st = ReservationStatus.valueOf(status);
                reservationPage = nurseReservationRepository
                        .findTodayByStatusWithFiltersPage(today, st, query, deptId, doctorId, src, pageable);
            } catch (IllegalArgumentException e) {
                reservationPage = nurseReservationRepository
                        .findTodayNonCancelledWithFiltersPage(today, ReservationStatus.CANCELLED, query, deptId, doctorId,
                                src, pageable);
            }
        }

        return reservationPage.map(r -> {
            long count = reservationRepository.countByPatient_IdAndStatus(r.getPatient().getId(), ReservationStatus.COMPLETED);
            return new NurseReservationDto(r, count == 0);
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
        Reservation r = nurseReservationRepository.findByIdWithDetails(reservationId)
                .orElseThrow(() -> CustomException.notFound("예약 정보를 찾을 수 없습니다."));
        long count = reservationRepository.countByPatient_IdAndStatus(r.getPatient().getId(), ReservationStatus.COMPLETED);
        return new NursePatientDto(r, count == 0);
    }

    @Transactional
    public void receiveReservation(Long reservationId) {
        Reservation r = nurseReservationRepository.findById(reservationId)
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