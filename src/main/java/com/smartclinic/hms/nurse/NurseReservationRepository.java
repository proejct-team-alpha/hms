package com.smartclinic.hms.nurse;

import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface NurseReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("SELECT r FROM Reservation r JOIN FETCH r.patient JOIN FETCH r.doctor d JOIN FETCH d.staff JOIN FETCH r.department WHERE r.reservationDate = :date AND r.status <> :excluded ORDER BY r.timeSlot")
    List<Reservation> findTodayNonCancelled(@Param("date") LocalDate date, @Param("excluded") ReservationStatus excluded);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.patient JOIN FETCH r.doctor d JOIN FETCH d.staff JOIN FETCH r.department WHERE r.reservationDate = :date AND r.status = :status ORDER BY r.timeSlot")
    List<Reservation> findTodayByStatus(@Param("date") LocalDate date, @Param("status") ReservationStatus status);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.patient JOIN FETCH r.doctor d JOIN FETCH d.staff JOIN FETCH r.department WHERE r.id = :id")
    Optional<Reservation> findByIdWithDetails(@Param("id") Long id);
}
