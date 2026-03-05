package com.smartclinic.hms.admin.reservation;

import com.smartclinic.hms.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface AdminReservationRepository extends JpaRepository<Reservation, Long> {

    long countByReservationDate(LocalDate reservationDate);
}
