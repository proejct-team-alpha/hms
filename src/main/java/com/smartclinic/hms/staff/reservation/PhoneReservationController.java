package com.smartclinic.hms.staff.reservation;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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
    public String phoneReservation() {
        return "staff/phone-reservation";
    }

    // 전화 예약 생성
    @PostMapping("/create")
    public String createPhoneReservation(PhoneReservationRequestDto request) {

        receptionService.createPhoneReservation(request);

        return "redirect:/staff/reception/list";
    }
}