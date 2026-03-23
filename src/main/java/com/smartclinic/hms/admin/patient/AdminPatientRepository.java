package com.smartclinic.hms.admin.patient;

import com.smartclinic.hms.domain.Patient;
import com.smartclinic.hms.domain.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AdminPatientRepository extends JpaRepository<Patient, Long> {

    @Query("""
            SELECT p
            FROM Patient p
            WHERE (:keyword = ''
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR REPLACE(p.phone, '-', '') LIKE CONCAT('%', :contactKeyword, '%'))
            ORDER BY p.createdAt DESC, p.id DESC
            """)
    Page<Patient> search(
            @Param("keyword") String keyword,
            @Param("contactKeyword") String contactKeyword,
            Pageable pageable);

    @Query("""
            SELECT r.reservationNumber AS reservationNumber,
                   r.reservationDate AS reservationDate,
                   r.timeSlot AS timeSlot,
                   department.name AS departmentName,
                   staff.name AS doctorName,
                   r.status AS status
            FROM Reservation r
            JOIN r.department department
            JOIN r.doctor doctor
            JOIN doctor.staff staff
            WHERE r.patient.id = :patientId
            ORDER BY r.reservationDate DESC, r.timeSlot DESC, r.id DESC
            """)
    List<AdminPatientReservationHistoryProjection> findReservationHistoriesByPatientId(@Param("patientId") Long patientId);

    @Query("""
            SELECT COUNT(p) > 0
            FROM Patient p
            WHERE p.id <> :patientId
              AND REPLACE(REPLACE(p.phone, '-', ''), ' ', '') = :normalizedPhone
            """)
    boolean existsByNormalizedPhoneAndIdNot(
            @Param("patientId") Long patientId,
            @Param("normalizedPhone") String normalizedPhone);

    interface AdminPatientReservationHistoryProjection {
        String getReservationNumber();

        LocalDate getReservationDate();

        String getTimeSlot();

        String getDepartmentName();

        String getDoctorName();

        ReservationStatus getStatus();
    }
}
