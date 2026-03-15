package com.smartclinic.hms.staff.reservation;

import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.smartclinic.hms.staff.reception.ReceptionService;
import com.smartclinic.hms.staff.reservation.dto.PhoneReservationRequestDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/staff/reservation")
public class PhoneReservationController {

    private final ReceptionService receptionService;

    // 전화 예약 등록 화면
    @GetMapping("/phone-reservation")
    public String phoneReservation(Model model) {
        model.addAttribute("today", LocalDate.now());
        return "staff/phone-reservation";
    }

    // 전화 예약 생성
    @PostMapping("/create")
    public String createPhoneReservation(@Valid PhoneReservationRequestDto request) {

        receptionService.createPhoneReservation(request);

        return "redirect:/staff/reception/list";
    }
}