package com.smartclinic.hms.admin.reservation;

import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query(value = """
            select r.id as id,
                   r.reservationNumber as reservationNumber,
                   r.reservationDate as reservationDate,
                   r.timeSlot as timeSlot,
                   patient.name as patientName,
                   patient.phone as patientPhone,
                   department.name as departmentName,
                   staff.name as doctorName,
                   r.status as status
            from Reservation r
            join r.patient patient
            join r.department department
            join r.doctor doctor
            join doctor.staff staff
            where (:status is null or r.status = :status)
              and (
                    :nameKeyword = ''
                    or lower(patient.name) like lower(concat('%', :nameKeyword, '%'))
                    or replace(patient.phone, '-', '') like concat('%', :phoneKeyword, '%')
                  )
            """, countQuery = """
            select count(r.id)
            from Reservation r
            join r.patient patient
            where (:status is null or r.status = :status)
              and (
                    :nameKeyword = ''
                    or lower(patient.name) like lower(concat('%', :nameKeyword, '%'))
                    or replace(patient.phone, '-', '') like concat('%', :phoneKeyword, '%')
                  )
            """)
    Page<AdminReservationListProjection> findReservationListPage(
            @Param("status") ReservationStatus status,
            @Param("nameKeyword") String nameKeyword,
            @Param("phoneKeyword") String phoneKeyword,
            Pageable pageable);

    interface DailyPatientCountProjection {
        LocalDate getDate();

        Long getPatientCount();
    }

    interface AdminReservationListProjection {
        Long getId();

        String getReservationNumber();

        LocalDate getReservationDate();

        String getTimeSlot();

        String getPatientName();

        String getPatientPhone();

        String getDepartmentName();

        String getDoctorName();

        ReservationStatus getStatus();
    }
}