package com.smartclinic.hms.nurse;

import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationSource;
import com.smartclinic.hms.domain.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface NurseReservationRepository extends JpaRepository<Reservation, Long> {

       @Query("SELECT r FROM Reservation r JOIN FETCH r.patient JOIN FETCH r.doctor d JOIN FETCH d.staff JOIN FETCH r.department WHERE r.reservationDate = :date AND r.status <> :excluded ORDER BY r.timeSlot")
       List<Reservation> findTodayNonCancelled(@Param("date") LocalDate date,
                     @Param("excluded") ReservationStatus excluded);

       @Query("SELECT r FROM Reservation r JOIN FETCH r.patient JOIN FETCH r.doctor d JOIN FETCH d.staff JOIN FETCH r.department WHERE r.reservationDate = :date AND r.status = :status ORDER BY r.timeSlot")
       List<Reservation> findTodayByStatus(@Param("date") LocalDate date, @Param("status") ReservationStatus status);

       @Query(value = "SELECT r FROM Reservation r JOIN FETCH r.patient JOIN FETCH r.doctor d JOIN FETCH d.staff JOIN FETCH r.department WHERE r.reservationDate = :date AND r.status <> :excluded ORDER BY r.timeSlot", countQuery = "SELECT count(r) FROM Reservation r WHERE r.reservationDate = :date AND r.status <> :excluded")
       Page<Reservation> findTodayNonCancelledPage(@Param("date") LocalDate date,
                     @Param("excluded") ReservationStatus excluded, Pageable pageable);

       @Query(value = "SELECT r FROM Reservation r JOIN FETCH r.patient JOIN FETCH r.doctor d JOIN FETCH d.staff JOIN FETCH r.department WHERE r.reservationDate = :date AND r.status = :status ORDER BY r.timeSlot", countQuery = "SELECT count(r) FROM Reservation r WHERE r.reservationDate = :date AND r.status = :status")
       Page<Reservation> findTodayByStatusPage(@Param("date") LocalDate date, @Param("status") ReservationStatus status,
                     Pageable pageable);

       @Query(value = "SELECT r FROM Reservation r JOIN FETCH r.patient p JOIN FETCH r.doctor d JOIN FETCH d.staff s JOIN FETCH r.department dep "
                     +
                     "WHERE r.reservationDate = :date AND r.status <> :excluded " +
                     "AND (:query IS NULL OR :query = '' OR p.name LIKE %:query% OR p.phone LIKE %:query% OR s.name LIKE %:query% OR dep.name LIKE %:query%) "
                     +
                     "AND (:deptId IS NULL OR dep.id = :deptId) " +
                     "AND (:doctorId IS NULL OR d.id = :doctorId) " +
                     "AND (:source IS NULL OR r.source = :source) " +
                     "ORDER BY r.timeSlot", countQuery = "SELECT count(r) FROM Reservation r JOIN r.patient p JOIN r.doctor d JOIN d.staff s JOIN r.department dep "
                                   +
                                   "WHERE r.reservationDate = :date AND r.status <> :excluded " +
                                   "AND (:query IS NULL OR :query = '' OR p.name LIKE %:query% OR p.phone LIKE %:query% OR s.name LIKE %:query% OR dep.name LIKE %:query%) "
                                   +
                                   "AND (:deptId IS NULL OR dep.id = :deptId) " +
                                   "AND (:doctorId IS NULL OR d.id = :doctorId) " +
                                   "AND (:source IS NULL OR r.source = :source)")
       Page<Reservation> findTodayNonCancelledWithFiltersPage(
                     @Param("date") LocalDate date,
                     @Param("excluded") ReservationStatus excluded,
                     @Param("query") String query,
                     @Param("deptId") Long deptId,
                     @Param("doctorId") Long doctorId,
                     @Param("source") ReservationSource source,
                     Pageable pageable);

       @Query(value = "SELECT r FROM Reservation r JOIN FETCH r.patient p JOIN FETCH r.doctor d JOIN FETCH d.staff s JOIN FETCH r.department dep "
                     +
                     "WHERE r.reservationDate = :date AND r.status = :status " +
                     "AND (:query IS NULL OR :query = '' OR p.name LIKE %:query% OR p.phone LIKE %:query% OR s.name LIKE %:query% OR dep.name LIKE %:query%) "
                     +
                     "AND (:deptId IS NULL OR dep.id = :deptId) " +
                     "AND (:doctorId IS NULL OR d.id = :doctorId) " +
                     "AND (:source IS NULL OR r.source = :source) " +
                     "ORDER BY r.timeSlot", countQuery = "SELECT count(r) FROM Reservation r JOIN r.patient p JOIN r.doctor d JOIN d.staff s JOIN r.department dep "
                                   +
                                   "WHERE r.reservationDate = :date AND r.status = :status " +
                                   "AND (:query IS NULL OR :query = '' OR p.name LIKE %:query% OR p.phone LIKE %:query% OR s.name LIKE %:query% OR dep.name LIKE %:query%) "
                                   +
                                   "AND (:deptId IS NULL OR dep.id = :deptId) " +
                                   "AND (:doctorId IS NULL OR d.id = :doctorId) " +
                                   "AND (:source IS NULL OR r.source = :source)")
       Page<Reservation> findTodayByStatusWithFiltersPage(
                     @Param("date") LocalDate date,
                     @Param("status") ReservationStatus status,
                     @Param("query") String query,
                     @Param("deptId") Long deptId,
                     @Param("doctorId") Long doctorId,
                     @Param("source") ReservationSource source,
                     Pageable pageable);

       @Query("SELECT r FROM Reservation r JOIN FETCH r.patient JOIN FETCH r.doctor d JOIN FETCH d.staff JOIN FETCH r.department WHERE r.id = :id")
       Optional<Reservation> findByIdWithDetails(@Param("id") Long id);
}
