package com.smartclinic.hms.reservation.reservation;

// [W2-#5 작업 목록]
// DONE 1. GET /api/reservation/departments — 진료과 전체 목록 API 추가

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartclinic.hms.doctor.DoctorDto;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reservation")
@RequiredArgsConstructor
public class ReservationApiController {

    private final ReservationService reservationService;

    @GetMapping("/departments")
    public List<DepartmentDto> getDepartments() {
        return reservationService.getDepartments();
    }

    @GetMapping("/doctors")
    public List<DoctorDto> getDoctors(@RequestParam("departmentId") Long departmentId) {
        return reservationService.getDoctorsByDepartment(departmentId);
    }
}
