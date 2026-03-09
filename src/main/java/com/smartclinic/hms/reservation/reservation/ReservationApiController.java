package com.smartclinic.hms.reservation.reservation;

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

    @GetMapping("/doctors")
    public List<DoctorDto> getDoctors(@RequestParam("departmentId") Long departmentId) {
        return reservationService.getDoctorsByDepartment(departmentId);
    }
}
