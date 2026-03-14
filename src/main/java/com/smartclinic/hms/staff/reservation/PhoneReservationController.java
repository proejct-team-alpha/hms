package com.smartclinic.hms.staff.reservation;

import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smartclinic.hms.staff.reception.ReceptionService;
import com.smartclinic.hms.staff.reservation.dto.PhoneReservationRequestDto;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/staff/reservation")
public class PhoneReservationController {

    private final ReceptionService receptionService;

    // 전화 예약 등록 화면
    @GetMapping("/phone-reservation")
    public String phoneReservation(Model model) {
        model.addAttribute("departments", receptionService.getAllDepartments());
        model.addAttribute("doctors", receptionService.getAllDoctors());
        model.addAttribute("today", LocalDate.now());
        return "staff/phone-reservation";
    }

    // 전화 예약 생성
    @PostMapping("/create")
    public String createPhoneReservation(PhoneReservationRequestDto request,
                                         RedirectAttributes redirectAttributes) {
        receptionService.createPhoneReservation(request);
        redirectAttributes.addFlashAttribute("message", "전화 예약이 완료되었습니다.");
        return "redirect:/staff/reception/list?date=" + request.getDate();
    }
}