package com.smartclinic.hms.staff.reception;

import java.util.List;

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

        reservation.receive();
    }

    @Transactional
    public List<Reservation> getReservations() {

        List<Reservation> reservations = reservationRepository.findAll();

        // LAZY 강제 로딩
        for (Reservation r : reservations) {
            r.getPatient().getName();
            r.getDoctor().getStaff().getName();
            r.getDepartment().getName();
        }

        return reservations;
    }

}
