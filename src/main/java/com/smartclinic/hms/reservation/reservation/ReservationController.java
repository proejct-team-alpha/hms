package com.smartclinic.hms.reservation.reservation;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.RequiredArgsConstructor;

@RequestMapping("/reservation")
@RequiredArgsConstructor
@Controller
public class ReservationController {

    private final ReservationService reservationService;

    @GetMapping("")
    public String patientChoice(HttpServletRequest request) {
        request.setAttribute("pageTitle", "진료 예약");
        return "reservation/patient-choice";
    }

    @GetMapping("/symptom-reservation")
    public String symptomReservation(Model model) {
        model.addAttribute("pageTitle", "AI 증상 분석 예약");
        return "reservation/symptom-reservation";
    }

    @GetMapping("/direct-reservation")
    public String directReservation(Model model) {
        model.addAttribute("pageTitle", "직접 선택 예약");
        return "reservation/direct-reservation";
    }

    @GetMapping("/complete")
    public String reservationComplete(Model model) {
        model.addAttribute("pageTitle", "예약 완료");
        return "reservation/reservation-complete";
    }
}
