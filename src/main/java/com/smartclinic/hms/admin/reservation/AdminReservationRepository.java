package com.smartclinic.hms.admin.reservation;

import com.smartclinic.hms.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AdminReservationRepository extends JpaRepository<Reservation, Long> {

    long countByReservationDate(LocalDate reservationDate);

    @Query("""
            select r.reservationDate as date, count(r.id) as patientCount
            from Reservation r
            where r.reservationDate between :startDate and :endDate
            group by r.reservationDate
            """)
    List<DailyPatientCountProjection> findDailyPatientCounts(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    interface DailyPatientCountProjection {
        LocalDate getDate();

        Long getPatientCount();
    }
}
