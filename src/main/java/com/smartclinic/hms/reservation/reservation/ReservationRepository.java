package com.smartclinic.hms.reservation.reservation;

// [W2-#4 작업 목록]
// DONE 1. JpaRepository<Reservation, Long> 구현

import com.smartclinic.hms.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
}
