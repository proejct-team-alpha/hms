package com.smartclinic.hms.admin.reservation;

import com.smartclinic.hms.admin.reservation.dto.AdminReservationListResponse;
import com.smartclinic.hms.common.exception.CustomException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(AdminReservationService.class)
@TestPropertySource(properties = "spring.sql.init.mode=never")
class AdminReservationServiceTest {

    private static final String RESERVATION_CANCELLED_MESSAGE = "예약이 취소되었습니다.";
    private static final String RECEPTION_CANCELLED_MESSAGE = "접수가 취소되었습니다.";

    @Autowired
    private AdminReservationService adminReservationService;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("status invalid value falls back to ALL")
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
    @DisplayName("status filter and default sort are applied")
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
    @DisplayName("received status uses 접수 label in option and row response")
    void receivedStatus_usesReceptionLabel() {
        // given
        persistReservation("RES-20260310-101", LocalDate.of(2026, 3, 10), "10:30", "RECEIVED");
        entityManager.flush();
        entityManager.clear();

        // when
        AdminReservationListResponse result = adminReservationService.getReservationList(1, 10, "RECEIVED");

        // then
        assertThat(result.selectedStatus()).isEqualTo("RECEIVED");
        assertThat(result.statusOptions())
                .filteredOn(option -> "RECEIVED".equals(option.value()))
                .singleElement()
                .satisfies(option -> {
                    assertThat(option.label()).isEqualTo("접수");
                    assertThat(option.selected()).isTrue();
                });
        assertThat(result.reservations()).singleElement().satisfies(item -> {
            assertThat(item.status()).isEqualTo("RECEIVED");
            assertThat(item.statusLabel()).isEqualTo("접수");
            assertThat(item.received()).isTrue();
        });
    }

    @Test
    @DisplayName("default paging is applied")
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

    @Test
    @DisplayName("예약 상태 취소 시 예약 취소 메시지를 반환한다")
    void cancelReservation_reserved_returnsReservationCancelledMessage() {
        // given
        persistReservation("RES-20260310-901", LocalDate.of(2026, 3, 10), "09:00", "RESERVED");
        entityManager.flush();
        entityManager.clear();

        Long reservationId = entityManager.createQuery(
                        "select r.id from Reservation r where r.reservationNumber = :number", Long.class)
                .setParameter("number", "RES-20260310-901")
                .getSingleResult();

        // when
        String result = adminReservationService.cancelReservation(reservationId);
        entityManager.flush();
        entityManager.clear();

        Reservation reservation = entityManager.find(Reservation.class, reservationId);

        // then
        assertThat(result).isEqualTo(RESERVATION_CANCELLED_MESSAGE);
        assertThat(reservation.getStatus().name()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("접수 상태 취소 시 접수 취소 메시지를 반환한다")
    void cancelReservation_received_returnsReceptionCancelledMessage() {
        // given
        persistReservation("RES-20260310-903", LocalDate.of(2026, 3, 10), "10:00", "RECEIVED");
        entityManager.flush();
        entityManager.clear();

        Long reservationId = entityManager.createQuery(
                        "select r.id from Reservation r where r.reservationNumber = :number", Long.class)
                .setParameter("number", "RES-20260310-903")
                .getSingleResult();

        // when
        String result = adminReservationService.cancelReservation(reservationId);
        entityManager.flush();
        entityManager.clear();

        Reservation reservation = entityManager.find(Reservation.class, reservationId);

        // then
        assertThat(result).isEqualTo(RECEPTION_CANCELLED_MESSAGE);
        assertThat(reservation.getStatus().name()).isEqualTo("RESERVED");
    }

    @Test
    @DisplayName("완료 상태 취소 시도는 예외가 발생한다")
    void cancelReservation_completed_throwsException() {
        // given
        persistReservation("RES-20260310-902", LocalDate.of(2026, 3, 10), "09:30", "COMPLETED");
        entityManager.flush();
        entityManager.clear();

        Long reservationId = entityManager.createQuery(
                        "select r.id from Reservation r where r.reservationNumber = :number", Long.class)
                .setParameter("number", "RES-20260310-902")
                .getSingleResult();

        // when
        // then
        assertThatThrownBy(() -> adminReservationService.cancelReservation(reservationId))
                .isInstanceOf(CustomException.class);
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
