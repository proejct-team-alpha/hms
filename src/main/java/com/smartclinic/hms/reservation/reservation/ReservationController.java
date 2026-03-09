package com.smartclinic.hms.reservation.reservation;

// [W2-#4 작업 목록]
// DONE 1. POST /reservation/create 추가 (PRG 패턴)
// DONE 2. 예약 저장 후 완료 화면으로 리다이렉트 (name, department, doctor, date, time 파라미터 전달)

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    public String directReservation(HttpServletRequest request) {
        request.setAttribute("pageTitle", "직접 선택 예약");
        return "reservation/direct-reservation";
    }

    @GetMapping("/complete")
    public String reservationComplete(Model model) {
        model.addAttribute("pageTitle", "예약 완료");
        return "reservation/reservation-complete";
    }

    @PostMapping("/create")
    public String createReservation(@ModelAttribute ReservationCreateForm form,
                                    RedirectAttributes redirectAttributes) {
        ReservationCompleteInfo info = reservationService.createReservation(form);

        redirectAttributes.addAttribute("name",       info.getPatientName());
        redirectAttributes.addAttribute("department", info.getDepartmentName());
        redirectAttributes.addAttribute("doctor",     info.getDoctorName());
        redirectAttributes.addAttribute("date",       info.getReservationDate());
        redirectAttributes.addAttribute("time",       info.getTimeSlot());

        return "redirect:/reservation/complete";
    }
}
