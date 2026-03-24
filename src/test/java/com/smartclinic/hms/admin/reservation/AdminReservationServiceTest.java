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
    @DisplayName("status invalid value falls back to ALL")
    void invalidStatus_fallbackToAll() {
        persistReservation("RES-20260310-001", LocalDate.of(2026, 3, 10), "09:00", "RESERVED", "홍길동", "010-1111-2222");
        persistReservation("RES-20260310-002", LocalDate.of(2026, 3, 10), "10:00", "RECEIVED", "김만두", "010-2222-3333");
        entityManager.flush();
        entityManager.clear();

        AdminReservationListResponse result = adminReservationService.getReservationList(1, 10, "INVALID", null);

        assertThat(result.selectedStatus()).isEqualTo("ALL");
        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.reservations()).hasSize(2);
    }

    @Test
    @DisplayName("status filter and default sort are applied")
    void statusFilterAndDefaultSort_applied() {
        persistReservation("RES-20260310-001", LocalDate.of(2026, 3, 10), "09:00", "RECEIVED", "환자1", "010-1111-1111");
        persistReservation("RES-20260310-002", LocalDate.of(2026, 3, 10), "11:00", "RECEIVED", "환자2", "010-2222-2222");
        persistReservation("RES-20260309-001", LocalDate.of(2026, 3, 9), "15:00", "RECEIVED", "환자3", "010-3333-3333");
        persistReservation("RES-20260310-003", LocalDate.of(2026, 3, 10), "08:00", "RESERVED", "환자4", "010-4444-4444");
        entityManager.flush();
        entityManager.clear();

        AdminReservationListResponse result = adminReservationService.getReservationList(1, 10, "RECEIVED", null);

        assertThat(result.selectedStatus()).isEqualTo("RECEIVED");
        assertThat(result.totalCount()).isEqualTo(3);
        assertThat(result.reservations()).extracting("reservationNumber")
                .containsExactly("RES-20260310-002", "RES-20260310-001", "RES-20260309-001");
    }

    @Test
    @DisplayName("received status uses 접수 label in option and row response")
    void receivedStatus_usesReceptionLabel() {
        persistReservation("RES-20260310-101", LocalDate.of(2026, 3, 10), "10:30", "RECEIVED", "홍길동", "010-1111-2222");
        entityManager.flush();
        entityManager.clear();

        AdminReservationListResponse result = adminReservationService.getReservationList(1, 10, "RECEIVED", null);

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
        for (int i = 1; i <= 12; i++) {
            persistReservation(String.format("RES-20260310-%03d", i), LocalDate.of(2026, 3, 10), String.format("%02d:00", i), "RESERVED", "환자" + i, "010-1000-" + String.format("%04d", i));
        }
        entityManager.flush();
        entityManager.clear();

        AdminReservationListResponse result = adminReservationService.getReservationList(1, 10, "ALL", null);

        assertThat(result.currentPage()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.reservations()).hasSize(10);
        assertThat(result.totalCount()).isEqualTo(12);
    }

    @Test
    @DisplayName("page links preserve status and keyword")
    void pageLinks_preserveStatusAndKeyword() {
        for (int i = 1; i <= 11; i++) {
            persistReservation(
                    String.format("RES-20260311-%03d", i),
                    LocalDate.of(2026, 3, 11),
                    String.format("%02d:00", i),
                    "RECEIVED",
                    "Kim Search",
                    "010-1200-" + String.format("%04d", i)
            );
        }
        entityManager.flush();
        entityManager.clear();

        AdminReservationListResponse result = adminReservationService.getReservationList(2, 5, "RECEIVED", "Kim");

        assertThat(result.currentPage()).isEqualTo(2);
        assertThat(result.hasPrevious()).isTrue();
        assertThat(result.hasNext()).isTrue();
        assertThat(result.previousUrl()).contains("page=1").contains("status=RECEIVED").contains("keyword=Kim");
        assertThat(result.nextUrl()).contains("page=3").contains("status=RECEIVED").contains("keyword=Kim");
        assertThat(result.pageLinks()).hasSize(3);
        assertThat(result.pageLinks()).allSatisfy(link -> {
            assertThat(link.url()).contains("size=5");
            assertThat(link.url()).contains("status=RECEIVED");
            assertThat(link.url()).contains("keyword=Kim");
        });
        assertThat(result.statusOptions()).allSatisfy(option -> {
            assertThat(option.url()).contains("size=5");
            assertThat(option.url()).contains("keyword=Kim");
        });
    }

    @Test
    @DisplayName("keyword search matches patient name")
    void keywordSearch_matchesPatientName() {
        persistReservation("RES-20260310-201", LocalDate.of(2026, 3, 10), "09:00", "RESERVED", "홍길동", "010-1111-2222");
        persistReservation("RES-20260310-202", LocalDate.of(2026, 3, 10), "10:00", "RESERVED", "김만두", "010-3333-4444");
        entityManager.flush();
        entityManager.clear();

        AdminReservationListResponse result = adminReservationService.getReservationList(1, 10, "ALL", "홍길동");

        assertThat(result.keyword()).isEqualTo("홍길동");
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.reservations()).singleElement().satisfies(item -> {
            assertThat(item.patientName()).isEqualTo("홍길동");
            assertThat(item.patientPhone()).isEqualTo("010-1111-2222");
        });
        assertThat(result.statusOptions()).allSatisfy(option -> assertThat(option.url()).contains("keyword=홍길동"));
        assertThat(result.pageLinks()).singleElement().satisfies(link -> assertThat(link.url()).contains("keyword=홍길동"));
    }

    @Test
    @DisplayName("keyword search matches patient phone ignoring hyphen")
    void keywordSearch_matchesPatientPhoneIgnoringHyphen() {
        persistReservation("RES-20260310-301", LocalDate.of(2026, 3, 10), "09:00", "RESERVED", "홍길동", "010-1234-5678");
        persistReservation("RES-20260310-302", LocalDate.of(2026, 3, 10), "10:00", "RESERVED", "김만두", "010-0000-0000");
        entityManager.flush();
        entityManager.clear();

        AdminReservationListResponse result = adminReservationService.getReservationList(1, 10, "ALL", "0101234");

        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.reservations()).singleElement().satisfies(item -> {
            assertThat(item.patientName()).isEqualTo("홍길동");
            assertThat(item.patientPhone()).isEqualTo("010-1234-5678");
        });
    }

    @Test
    @DisplayName("status and keyword filters can be combined")
    void statusAndKeyword_filtersCanBeCombined() {
        persistReservation("RES-20260310-401", LocalDate.of(2026, 3, 10), "09:00", "RESERVED", "홍길동", "010-1111-2222");
        persistReservation("RES-20260310-402", LocalDate.of(2026, 3, 10), "10:00", "RECEIVED", "홍길동", "010-1111-2222");
        entityManager.flush();
        entityManager.clear();

        AdminReservationListResponse result = adminReservationService.getReservationList(1, 10, "RECEIVED", "홍길동");

        assertThat(result.selectedStatus()).isEqualTo("RECEIVED");
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.reservations()).singleElement().satisfies(item -> assertThat(item.status()).isEqualTo("RECEIVED"));
    }

    @Test
    @DisplayName("reservation row includes patient detail navigation info")
    void reservationRow_includesPatientDetailNavigationInfo() {
        persistReservation("RES-20260310-501", LocalDate.of(2026, 3, 10), "09:00", "RESERVED", "홍길동", "010-1111-2222");
        entityManager.flush();
        entityManager.clear();

        AdminReservationListResponse result = adminReservationService.getReservationList(1, 10, "ALL", null);

        assertThat(result.reservations()).singleElement().satisfies(item -> {
            assertThat(item.patientId()).isNotNull();
            assertThat(item.patientDetailUrl()).isEqualTo("/admin/patient/detail?patientId=" + item.patientId());
        });
    }

    private void persistReservation(String reservationNumber, LocalDate date, String timeSlot, String status, String patientName, String patientPhone) {
        Department department = Department.create("진료과-" + reservationNumber, true);
        entityManager.persist(department);

        Staff doctorStaff = Staff.create(
                "doctor-" + reservationNumber,
                "D-" + reservationNumber,
                "{noop}pw",
                "의사-" + reservationNumber,
                StaffRole.DOCTOR,
                department
        );
        entityManager.persist(doctorStaff);

        Doctor doctor = Doctor.create(doctorStaff, department, "MON,TUE,WED,THU,FRI", "전문의");
        entityManager.persist(doctor);

        Patient patient = Patient.create(patientName, patientPhone, reservationNumber + "@test.com");
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
