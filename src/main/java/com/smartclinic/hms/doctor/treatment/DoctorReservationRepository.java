package com.smartclinic.hms.doctor.treatment;

import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DoctorReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("SELECT r FROM Reservation r JOIN FETCH r.patient WHERE r.doctor.staff.username = :username AND r.reservationDate = :date AND r.status <> :excluded ORDER BY r.timeSlot")
    List<Reservation> findTodayActiveByDoctor(
            @Param("username") String username,
            @Param("date") LocalDate date,
            @Param("excluded") ReservationStatus excluded);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.patient WHERE r.doctor.staff.username = :username AND r.reservationDate = :date AND r.status = :status ORDER BY r.timeSlot DESC")
    List<Reservation> findTodayByDoctorAndStatus(
            @Param("username") String username,
            @Param("date") LocalDate date,
            @Param("status") ReservationStatus status);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.patient WHERE r.id = :id AND r.doctor.staff.username = :username")
    Optional<Reservation> findByIdAndDoctor(
            @Param("id") Long id,
            @Param("username") String username);
}
