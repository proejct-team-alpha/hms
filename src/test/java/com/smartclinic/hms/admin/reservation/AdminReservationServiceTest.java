package com.smartclinic.hms.admin.reservation;

import com.smartclinic.hms.admin.reservation.dto.AdminReservationListResponse;
import com.smartclinic.hms.domain.Department;
import com.smartclinic.hms.domain.Doctor;
import com.smartclinic.hms.domain.Patient;
import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationSource;
import com.smartclinic.hms.domain.Staff;
import com.smartclinic.hms.domain.StaffRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(AdminReservationService.class)
@TestPropertySource(properties = "spring.sql.init.mode=never")
class AdminReservationServiceTest {

    @Autowired
    private AdminReservationService adminReservationService;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("status媛 ?섎せ??媛믪씠硫?ALL濡?fallback ?쒕떎")
    void invalidStatus_fallbackToAll() {
        // given
        persistReservation("RES-20260310-001", LocalDate.of(2026, 3, 10), "09:00", "RESERVED");
        persistReservation("RES-20260310-002", LocalDate.of(2026, 3, 10), "10:00", "RECEIVED");
        entityManager.flush();
        entityManager.clear();

        // when
        AdminReservationListResponse result = adminReservationService.getReservationList(1, 10, "INVALID");

        // then
        assertThat(result.selectedStatus()).isEqualTo("ALL");
        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.reservations()).hasSize(2);
    }

    @Test
    @DisplayName("?곹깭 ?꾪꽣? 湲곕낯 ?뺣젹(reservationDate DESC, timeSlot DESC)???곸슜?쒕떎")
    void statusFilterAndDefaultSort_applied() {
        // given
        persistReservation("RES-20260310-001", LocalDate.of(2026, 3, 10), "09:00", "RECEIVED");
        persistReservation("RES-20260310-002", LocalDate.of(2026, 3, 10), "11:00", "RECEIVED");
        persistReservation("RES-20260309-001", LocalDate.of(2026, 3, 9), "15:00", "RECEIVED");
        persistReservation("RES-20260310-003", LocalDate.of(2026, 3, 10), "08:00", "RESERVED");
        entityManager.flush();
        entityManager.clear();

        // when
        AdminReservationListResponse result = adminReservationService.getReservationList(1, 10, "RECEIVED");

        // then
        assertThat(result.selectedStatus()).isEqualTo("RECEIVED");
        assertThat(result.totalCount()).isEqualTo(3);
        assertThat(result.reservations()).extracting("reservationNumber")
                .containsExactly("RES-20260310-002", "RES-20260310-001", "RES-20260309-001");
    }

    @Test
    @DisplayName("湲곕낯 ?섏씠吏?page=1, size=10)???곸슜?쒕떎")
    void defaultPaging_applied() {
        // given
        for (int i = 1; i <= 12; i++) {
            persistReservation(String.format("RES-20260310-%03d", i), LocalDate.of(2026, 3, 10), String.format("%02d:00", i), "RESERVED");
        }
        entityManager.flush();
        entityManager.clear();

        // when
        AdminReservationListResponse result = adminReservationService.getReservationList(1, 10, "ALL");

        // then
        assertThat(result.currentPage()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.reservations()).hasSize(10);
        assertThat(result.totalCount()).isEqualTo(12);
    }

    private void persistReservation(String reservationNumber, LocalDate date, String timeSlot, String status) {
        Department department = Department.create("?닿낵-" + reservationNumber, true);
        entityManager.persist(department);

        Staff doctorStaff = Staff.create(
                "doctor-" + reservationNumber,
                "D-" + reservationNumber,
                "{noop}pw",
                "?섏궗-" + reservationNumber,
                StaffRole.DOCTOR,
                department
        );
        entityManager.persist(doctorStaff);

        Doctor doctor = Doctor.create(doctorStaff, department, "MON,TUE,WED,THU,FRI", "?닿낵");
        entityManager.persist(doctor);

        Patient patient = Patient.create("?섏옄-" + reservationNumber, "010-1111-2222", reservationNumber + "@test.com");
        entityManager.persist(patient);

        Reservation reservation = Reservation.create(
                reservationNumber,
                patient,
                doctor,
                department,
                date,
                timeSlot,
                ReservationSource.ONLINE
        );
        entityManager.persist(reservation);

        if ("RECEIVED".equals(status)) {
            reservation.receive();
        }
        if ("COMPLETED".equals(status)) {
            reservation.receive();
            reservation.complete();
        }
        if ("CANCELLED".equals(status)) {
            reservation.cancel();
        }
    }
}
