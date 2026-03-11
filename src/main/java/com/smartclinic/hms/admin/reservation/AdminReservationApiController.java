package com.smartclinic.hms.admin.reservation;

import com.smartclinic.hms.admin.reservation.dto.AdminReservationCancelResponse;
import com.smartclinic.hms.common.util.Resp;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reservations")
public class AdminReservationApiController {

    private static final String CANCELLED_STATUS = "CANCELLED";

    private final AdminReservationService adminReservationService;

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Resp<AdminReservationCancelResponse>> cancelReservation(
            @PathVariable("id") Long reservationId) {
        adminReservationService.cancelReservation(reservationId);

        AdminReservationCancelResponse response = new AdminReservationCancelResponse(
                reservationId,
                CANCELLED_STATUS);

        return Resp.ok(response);
    }
}