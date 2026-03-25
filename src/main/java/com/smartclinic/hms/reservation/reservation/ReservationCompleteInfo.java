package com.smartclinic.hms.reservation.reservation;

import java.io.Serial;
import java.io.Serializable;

// DTO for reservation completion page flash attributes.
public record ReservationCompleteInfo(
        String reservationNumber,
        String patientName,
        String departmentName,
        String doctorName,
        String reservationDate,
        String timeSlot
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
