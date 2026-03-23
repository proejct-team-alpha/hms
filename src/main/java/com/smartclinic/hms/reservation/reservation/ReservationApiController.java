package com.smartclinic.hms.reservation.reservation;

// [W2-#5 작업 목록]
// DONE 1. GET /api/reservation/departments — 진료과 전체 목록 API 추가

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartclinic.hms.common.util.Resp;
import com.smartclinic.hms.doctor.DoctorDto;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reservation")
@RequiredArgsConstructor
public class ReservationApiController {

    private final ReservationService reservationService;

    // 진료과 전체 목록 (예약 폼 드롭다운용)
    @GetMapping("/departments")
    public ResponseEntity<Resp<List<DepartmentDto>>> getDepartments() {
        return Resp.ok(reservationService.getDepartments());
    }

    // 진료과별 의사 목록 (AJAX 드롭다운용)
    @GetMapping("/doctors")
    public ResponseEntity<Resp<List<DoctorDto>>> getDoctors(@RequestParam("departmentId") Long departmentId) {
        return Resp.ok(reservationService.getDoctorsByDepartment(departmentId));
    }

    // 예약된 시간 슬롯 조회 (Flatpickr 비활성화용, excludeId 있으면 변경 중인 예약 제외)
    @GetMapping("/booked-slots")
    public ResponseEntity<Resp<List<String>>> getBookedSlots(
            @RequestParam("doctorId") Long doctorId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "excludeId", required = false) Long excludeId) {
        if (excludeId != null) {
            return Resp.ok(reservationService.getBookedTimeSlots(doctorId, date, excludeId));
        }
        return Resp.ok(reservationService.getBookedTimeSlots(doctorId, date));
    }
}
