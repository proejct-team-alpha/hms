package com.smartclinic.hms.staff.reception;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.reservation.reservation.ReservationRepository;
import com.smartclinic.hms.staff.reception.dto.ReceptionUpdateRequest;
import com.smartclinic.hms.staff.reservation.dto.PhoneReservationRequestDto;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class ReceptionService {

    private final ReservationRepository reservationRepository;

    @Transactional
    public void receive(ReceptionUpdateRequest request) {

        Reservation reservation = reservationRepository.findById(request.getReservationId())
                .orElseThrow(() -> new RuntimeException("예약 없음"));

        // 접수 상태 변경
        reservation.receive();
    }

    public void createPhoneReservation(PhoneReservationRequestDto request) {

        System.out.println(request.getName());
        System.out.println(request.getPhone());
        System.out.println(request.getDepartment());

    }

}
