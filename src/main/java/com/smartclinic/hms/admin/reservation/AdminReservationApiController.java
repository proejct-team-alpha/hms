package com.smartclinic.hms.admin.reservation;

import com.smartclinic.hms.admin.reservation.dto.AdminReservationCancelDataResponse;
import com.smartclinic.hms.admin.reservation.dto.AdminReservationCancelResponse;
import com.smartclinic.hms.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reservations")
public class AdminReservationApiController {

    private static final String CANCEL_SUCCESS_MESSAGE = "예약이 취소되었습니다.";
    private static final String CANCELLED_STATUS = "CANCELLED";

    private final AdminReservationService adminReservationService;

    @PostMapping("/{id}/cancel")
    public ResponseEntity<AdminReservationCancelResponse> cancelReservation(
            @PathVariable("id") Long reservationId,
            Authentication authentication) {
        validateAdminRole(authentication);
        adminReservationService.cancelReservation(reservationId);

        AdminReservationCancelResponse response = new AdminReservationCancelResponse(
                true,
                new AdminReservationCancelDataResponse(reservationId, CANCELLED_STATUS),
                CANCEL_SUCCESS_MESSAGE);
        return ResponseEntity.ok(response);
    }

    private void validateAdminRole(Authentication authentication) {
        if (authentication == null) {
            throw CustomException.unauthorized("\uB85C\uADF8\uC778\uC774 \uD544\uC694\uD569\uB2C8\uB2E4.");
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        if (!isAdmin) {
            throw CustomException
                    .forbidden("\uAD00\uB9AC\uC790 \uAD8C\uD55C\uB9CC \uC811\uADFC \uAC00\uB2A5\uD569\uB2C8\uB2E4.");
        }
    }
}
