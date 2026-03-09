package com.smartclinic.hms.reservation.reservation;

import com.smartclinic.hms.doctor.DoctorDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservation")
@RequiredArgsConstructor
public class ReservationApiController {

    private final ReservationService reservationService;

    @GetMapping("/doctors")
    public List<DoctorDto> getDoctors(@RequestParam Long departmentId) {
        return reservationService.getDoctorsByDepartment(departmentId);
    }
}
