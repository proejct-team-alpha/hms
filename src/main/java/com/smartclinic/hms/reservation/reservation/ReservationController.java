package com.smartclinic.hms.reservation.reservation;

// [W2-#4 작업 목록]
// DONE 1. POST /reservation/create 추가 (PRG 패턴)
// DONE 2. 예약 저장 후 완료 화면으로 리다이렉트 (name, department, doctor, date, time 파라미터 전달)

// [W2-#5 작업 목록]
// DONE 1. GET /reservation/cancel — 예약번호 조회 + 취소 확인 화면
// DONE 2. POST /reservation/cancel/{id} — 취소 처리 후 redirect:/reservation
// DONE 3. GET /reservation/modify — 예약번호 조회 + 변경 폼 화면
// DONE 4. POST /reservation/modify/{id} — 변경 처리 후 redirect:/reservation/complete

// [W2-#5.1 작업 목록]
// DONE 1. POST /reservation/create — reservationNumber RedirectAttributes 추가
// DONE 2. GET /reservation/lookup — 예약번호 단건 / 이름+전화번호 목록 조회

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

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

        redirectAttributes.addAttribute("reservationNumber", info.getReservationNumber());
        redirectAttributes.addAttribute("name",       info.getPatientName());
        redirectAttributes.addAttribute("department", info.getDepartmentName());
        redirectAttributes.addAttribute("doctor",     info.getDoctorName());
        redirectAttributes.addAttribute("date",       info.getReservationDate());
        redirectAttributes.addAttribute("time",       info.getTimeSlot());

        return "redirect:/reservation/complete";
    }

    @GetMapping("/lookup")
    public String lookupPage(HttpServletRequest request) {
        String reservationNumber = request.getParameter("reservationNumber");
        String name  = request.getParameter("name");
        String phone = request.getParameter("phone");

        if (reservationNumber != null && !reservationNumber.isBlank()) {
            reservationService.findByReservationNumber(reservationNumber)
                    .ifPresentOrElse(
                            dto -> request.setAttribute("reservation", dto),
                            () -> request.setAttribute("errorMessage", "예약을 찾을 수 없습니다.")
                    );
        } else if (name != null && !name.isBlank() && phone != null && !phone.isBlank()) {
            List<ReservationInfoDto> list = reservationService.findByPhoneAndName(phone, name);
            if (list.isEmpty()) {
                request.setAttribute("errorMessage", "예약을 찾을 수 없습니다.");
            } else {
                request.setAttribute("reservations", list);
            }
        }
        request.setAttribute("pageTitle", "예약 조회");
        return "reservation/reservation-lookup";
    }

    @GetMapping("/cancel")
    public String cancelPage(HttpServletRequest request) {
        String reservationNumber = request.getParameter("reservationNumber");
        if (reservationNumber != null && !reservationNumber.isBlank()) {
            reservationService.findByReservationNumber(reservationNumber)
                    .ifPresentOrElse(
                            r -> request.setAttribute("reservation", r),
                            () -> request.setAttribute("errorMessage", "예약을 찾을 수 없습니다.")
                    );
        }
        request.setAttribute("pageTitle", "예약 취소");
        return "reservation/reservation-cancel";
    }

    @PostMapping("/cancel/{id}")
    public String cancelReservation(@PathVariable("id") Long id,
                                    RedirectAttributes redirectAttributes) {
        ReservationCompleteInfo info = reservationService.cancelReservation(id);
        redirectAttributes.addAttribute("reservationNumber", info.getReservationNumber());
        redirectAttributes.addAttribute("name",       info.getPatientName());
        redirectAttributes.addAttribute("department", info.getDepartmentName());
        redirectAttributes.addAttribute("doctor",     info.getDoctorName());
        redirectAttributes.addAttribute("date",       info.getReservationDate());
        redirectAttributes.addAttribute("time",       info.getTimeSlot());
        return "redirect:/reservation/cancel-complete";
    }

    @GetMapping("/cancel-complete")
    public String cancelComplete(HttpServletRequest request) {
        request.setAttribute("pageTitle", "예약 취소 완료");
        return "reservation/reservation-cancel-complete";
    }

    @GetMapping("/modify")
    public String modifyPage(HttpServletRequest request) {
        String reservationNumber = request.getParameter("reservationNumber");
        if (reservationNumber != null && !reservationNumber.isBlank()) {
            reservationService.findByReservationNumber(reservationNumber)
                    .ifPresentOrElse(
                            r -> request.setAttribute("reservation", r),
                            () -> request.setAttribute("errorMessage", "예약을 찾을 수 없습니다.")
                    );
        }
        request.setAttribute("pageTitle", "예약 변경");
        return "reservation/reservation-modify";
    }

    @PostMapping("/modify/{id}")
    public String modifyReservation(@PathVariable("id") Long id,
                                    @ModelAttribute ReservationUpdateForm form,
                                    RedirectAttributes redirectAttributes) {
        ReservationCompleteInfo info = reservationService.updateReservation(id, form);
        redirectAttributes.addAttribute("reservationNumber", info.getReservationNumber());
        redirectAttributes.addAttribute("name",       info.getPatientName());
        redirectAttributes.addAttribute("department", info.getDepartmentName());
        redirectAttributes.addAttribute("doctor",     info.getDoctorName());
        redirectAttributes.addAttribute("date",       info.getReservationDate());
        redirectAttributes.addAttribute("time",       info.getTimeSlot());
        return "redirect:/reservation/modify-complete";
    }

    @GetMapping("/modify-complete")
    public String modifyComplete(HttpServletRequest request) {
        request.setAttribute("pageTitle", "예약 변경 완료");
        return "reservation/reservation-modify-complete";
    }
}
