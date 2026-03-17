package com.smartclinic.hms.staff.reservation;

import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smartclinic.hms.staff.reception.ReceptionService;
import com.smartclinic.hms.staff.reservation.dto.PhoneReservationRequestDto;

import jakarta.validation.Valid;

import com.smartclinic.hms.common.exception.CustomException;

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
    public String createPhoneReservation(
            @Valid PhoneReservationRequestDto request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {

        // 1. DTO 검증 실패 처리
        if (bindingResult.hasErrors()) {
            model.addAttribute("form", request);
            model.addAttribute("departments", receptionService.getAllDepartments());
            model.addAttribute("doctors", receptionService.getAllDoctors());
            model.addAttribute("today", LocalDate.now());
            return "staff/phone-reservation";
        }

        try {
            boolean nameMismatch = receptionService.createPhoneReservation(request);

            if (nameMismatch) {
                redirectAttributes.addFlashAttribute("message",
                        "입력하신 이름과 기존 환자의 이름이 다릅니다. 기존 환자명으로 예약이 완료되었습니다.");
            } else {
                redirectAttributes.addFlashAttribute("message",
                        "전화 예약이 완료되었습니다.");
            }

            return "redirect:/staff/reception/list?date=" + request.getDate();

        } catch (CustomException e) {
            // 중복 예약 등 에러 발생 시 원래의 예약 폼으로 돌아가며 에러 메시지와 입력값을 유지
            model.addAttribute("message", e.getMessage());
            model.addAttribute("form", request); // 입력된 폼 데이터 유지
            model.addAttribute("departments", receptionService.getAllDepartments());
            model.addAttribute("doctors", receptionService.getAllDoctors());
            model.addAttribute("today", LocalDate.now());
            return "staff/phone-reservation"; // 리다이렉트 대신 폼 뷰를 직접 반환
        }
    }
}