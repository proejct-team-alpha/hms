package com.smartclinic.hms.reservation.reservation;

import org.springframework.stereotype.Controller;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Controller
public class ReservationController {

    private final ReservationService reservationService;

}
