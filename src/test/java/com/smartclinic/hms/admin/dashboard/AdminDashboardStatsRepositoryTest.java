package com.smartclinic.hms.admin.dashboard;

import com.smartclinic.hms.admin.item.ItemRepository;
import com.smartclinic.hms.admin.reservation.AdminReservationRepository;
import com.smartclinic.hms.admin.staff.AdminStaffRepository;
import com.smartclinic.hms.domain.Department;
import com.smartclinic.hms.domain.Doctor;
import com.smartclinic.hms.domain.Item;
import com.smartclinic.hms.domain.ItemCategory;
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
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.sql.init.mode=never")
class AdminDashboardStatsRepositoryTest {

    @Autowired
    private AdminReservationRepository adminReservationRepository;

    @Autowired
    private AdminStaffRepository adminStaffRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("집계 쿼리 4종이 엔티티 데이터 기준으로 동작한다")
    void aggregateQueries_returnExpectedCounts() {
        // given
        LocalDate today = LocalDate.of(2026, 3, 5);
        Department department = Department.create("통계내과", true);
        entityManager.persist(department);

        Staff doctorStaff = Staff.create(
                "doctor-stats", "D-STAT-001", "{noop}pw", "의사",
                StaffRole.DOCTOR, department
        );
        Staff activeStaff = Staff.create(
                "staff-active", "S-STAT-001", "{noop}pw", "활성직원",
                StaffRole.STAFF, department
        );
        Staff inactiveStaff = Staff.create(
                "staff-inactive", "S-STAT-002", "{noop}pw", "비활성직원",
                StaffRole.STAFF, department
        );
        inactiveStaff.update("비활성직원", department, false);

        entityManager.persist(doctorStaff);
        entityManager.persist(activeStaff);
        entityManager.persist(inactiveStaff);

        Doctor doctor = Doctor.create(doctorStaff, department, "MON,TUE,WED,THU,FRI", "내과");
        entityManager.persist(doctor);

        Patient patient = Patient.create("환자", "010-1111-2222", "patient@test.com");
        entityManager.persist(patient);

        Reservation todayReservation = Reservation.create(
                "RES-20260305-001", patient, doctor, department, today, "09:00", ReservationSource.ONLINE
        );
        Reservation yesterdayReservation = Reservation.create(
                "RES-20260304-001", patient, doctor, department, today.minusDays(1), "10:00", ReservationSource.PHONE
        );
        entityManager.persist(todayReservation);
        entityManager.persist(yesterdayReservation);

        Item lowStock = Item.create("붕대", ItemCategory.MEDICAL_SUPPLIES, 2, 5);
        Item enoughStock = Item.create("주사기", ItemCategory.MEDICAL_SUPPLIES, 20, 10);
        entityManager.persist(lowStock);
        entityManager.persist(enoughStock);

        entityManager.flush();
        entityManager.clear();

        // when
        long todayReservations = adminReservationRepository.countByReservationDate(today);
        long totalReservations = adminReservationRepository.count();
        long totalActiveStaff = adminStaffRepository.countByActiveTrue();
        long lowStockItems = itemRepository.countLowStockItems();

        // then
        assertThat(todayReservations).isEqualTo(1L);
        assertThat(totalReservations).isEqualTo(2L);
        assertThat(totalActiveStaff).isEqualTo(2L);
        assertThat(lowStockItems).isEqualTo(1L);
    }
}
