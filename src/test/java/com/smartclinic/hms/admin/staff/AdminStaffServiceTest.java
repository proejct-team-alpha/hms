package com.smartclinic.hms.admin.staff;

import com.smartclinic.hms.admin.staff.dto.AdminStaffListResponse;
import com.smartclinic.hms.admin.staff.dto.CreateAdminStaffRequest;
import com.smartclinic.hms.admin.staff.dto.UpdateAdminStaffRequest;
import com.smartclinic.hms.common.exception.CustomException;
import com.smartclinic.hms.domain.Department;
import com.smartclinic.hms.domain.Doctor;
import com.smartclinic.hms.domain.Staff;
import com.smartclinic.hms.domain.StaffRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({AdminStaffService.class, AdminStaffServiceTest.PasswordEncoderTestConfig.class})
@TestPropertySource(properties = "spring.sql.init.mode=never")
class AdminStaffServiceTest {

    @Autowired
    private AdminStaffService adminStaffService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("applies list filters")
    void getStaffList_appliesKeywordRoleAndEmploymentFilters() {
        // given
        Department internalMedicine = persistDepartment("Internal Medicine");
        Department surgery = persistDepartment("Surgery");

        persistStaff("kim-doctor", "D-001", "Kim Doctor", StaffRole.DOCTOR, internalMedicine, true);
        persistStaff("kim-nurse", "N-001", "Kim Nurse", StaffRole.NURSE, internalMedicine, true);
        persistStaff("lee-doctor", "D-002", "Lee Doctor", StaffRole.DOCTOR, surgery, false);

        entityManager.flush();
        entityManager.clear();

        // when
        AdminStaffListResponse result = adminStaffService.getStaffList(1, 10, "kim", "DOCTOR", "ACTIVE", "admin01");

        // then
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.staffs()).hasSize(1);
        assertThat(result.staffs().getFirst().name()).isEqualTo("Kim Doctor");
        assertThat(result.staffs().getFirst().role()).isEqualTo("DOCTOR");
        assertThat(result.staffs().getFirst().active()).isTrue();
        assertThat(result.staffs().getFirst().deactivatable()).isTrue();
    }

    @Test
    @DisplayName("encrypts password on create")
    void createStaff_encryptsPasswordAndSaves() {
        // given
        Department department = persistDepartment("Internal Medicine");
        CreateAdminStaffRequest request = new CreateAdminStaffRequest(
                "staff-new",
                "password123",
                "New Staff",
                "S-NEW-001",
                "STAFF",
                department.getId(),
                true,
                null,
                List.of()
        );

        // when
        adminStaffService.createStaff(request);
        entityManager.flush();
        entityManager.clear();

        Staff savedStaff = entityManager.createQuery(
                        "select s from Staff s where s.username = :username", Staff.class)
                .setParameter("username", "staff-new")
                .getSingleResult();

        // then
        assertThat(savedStaff.getName()).isEqualTo("New Staff");
        assertThat(savedStaff.isActive()).isTrue();
        assertThat(savedStaff.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", savedStaff.getPassword())).isTrue();
    }

    @Test
    @DisplayName("creates doctor entity when role is doctor")
    void createStaff_createsDoctorWhenRoleIsDoctor() {
        // given
        Department department = persistDepartment("Family Medicine");
        CreateAdminStaffRequest request = new CreateAdminStaffRequest(
                "doctor-new",
                "password123",
                "New Doctor",
                "D-NEW-001",
                "DOCTOR",
                department.getId(),
                true,
                "family medicine",
                List.of("MON", "WED", "FRI")
        );

        // when
        adminStaffService.createStaff(request);
        entityManager.flush();
        entityManager.clear();

        Staff savedStaff = entityManager.createQuery(
                        "select s from Staff s join fetch s.department where s.username = :username", Staff.class)
                .setParameter("username", "doctor-new")
                .getSingleResult();
        Doctor savedDoctor = entityManager.createQuery(
                        "select d from Doctor d join fetch d.staff join fetch d.department where d.staff.id = :staffId", Doctor.class)
                .setParameter("staffId", savedStaff.getId())
                .getSingleResult();

        // then
        assertThat(savedStaff.getRole()).isEqualTo(StaffRole.DOCTOR);
        assertThat(savedDoctor.getStaff().getId()).isEqualTo(savedStaff.getId());
        assertThat(savedDoctor.getDepartment().getId()).isEqualTo(department.getId());
        assertThat(savedDoctor.getSpecialty()).isEqualTo("family medicine");
        assertThat(savedDoctor.getAvailableDays()).isEqualTo("MON,WED,FRI");
    }

    @Test
    @DisplayName("throws when username is duplicated")
    void createStaff_duplicateUsername_throwsException() {
        // given
        Department department = persistDepartment("Surgery");
        persistStaff("duplicate-user", "S-001", "Existing Staff", StaffRole.STAFF, department, true);
        entityManager.flush();
        entityManager.clear();

        CreateAdminStaffRequest request = new CreateAdminStaffRequest(
                "duplicate-user",
                "password123",
                "New Staff",
                "S-002",
                "STAFF",
                department.getId(),
                true,
                null,
                List.of()
        );

        // when
        // then
        assertThatThrownBy(() -> adminStaffService.createStaff(request))
                .isInstanceOf(CustomException.class)
                .hasMessage("Username is already in use.");
    }

    @Test
    @DisplayName("updates name department and password")
    void updateStaff_updatesNameDepartmentAndPassword() {
        // given
        Department originalDepartment = persistDepartment("General Affairs");
        Department changedDepartment = persistDepartment("Administration");
        Staff staff = persistStaff("staff-update", "S-010", "Existing Staff", StaffRole.STAFF, originalDepartment, true);
        entityManager.flush();
        entityManager.clear();

        UpdateAdminStaffRequest request = new UpdateAdminStaffRequest(
                staff.getId(),
                "Updated Staff",
                changedDepartment.getId(),
                "newpassword123",
                null,
                List.of()
        );

        // when
        adminStaffService.updateStaff(request);
        entityManager.flush();
        entityManager.clear();

        Staff updatedStaff = entityManager.find(Staff.class, staff.getId());

        // then
        assertThat(updatedStaff.getName()).isEqualTo("Updated Staff");
        assertThat(updatedStaff.getDepartment().getId()).isEqualTo(changedDepartment.getId());
        assertThat(passwordEncoder.matches("newpassword123", updatedStaff.getPassword())).isTrue();
    }

    @Test
    @DisplayName("updates doctor specialty and available days")
    void updateStaff_updatesDoctorFields() {
        // given
        Department internalMedicine = persistDepartment("Internal Medicine");
        Department familyMedicine = persistDepartment("Family Medicine");
        Staff doctorStaff = persistStaff("doctor-user", "D-100", "Kim Doctor", StaffRole.DOCTOR, internalMedicine, true);
        Doctor doctor = Doctor.create(doctorStaff, internalMedicine, "MON,WED", "digestive");
        entityManager.persist(doctor);
        entityManager.flush();
        entityManager.clear();

        UpdateAdminStaffRequest request = new UpdateAdminStaffRequest(
                doctorStaff.getId(),
                "Kim Family Doctor",
                familyMedicine.getId(),
                "",
                "family medicine",
                List.of("TUE", "THU", "SAT")
        );

        // when
        adminStaffService.updateStaff(request);
        entityManager.flush();
        entityManager.clear();

        Staff updatedStaff = entityManager.find(Staff.class, doctorStaff.getId());
        Doctor updatedDoctor = entityManager.createQuery(
                        "select d from Doctor d join fetch d.department where d.staff.id = :staffId", Doctor.class)
                .setParameter("staffId", doctorStaff.getId())
                .getSingleResult();

        // then
        assertThat(updatedStaff.getName()).isEqualTo("Kim Family Doctor");
        assertThat(updatedStaff.getDepartment().getId()).isEqualTo(familyMedicine.getId());
        assertThat(updatedDoctor.getDepartment().getId()).isEqualTo(familyMedicine.getId());
        assertThat(updatedDoctor.getSpecialty()).isEqualTo("family medicine");
        assertThat(updatedDoctor.getAvailableDays()).isEqualTo("TUE,THU,SAT");
    }

    @Test
    @DisplayName("deactivates staff")
    void deactivateStaff_updatesActiveToFalse() {
        // given
        Department department = persistDepartment("Internal Medicine");
        Staff target = persistStaff("staff-target", "S-300", "Target Staff", StaffRole.STAFF, department, true);
        entityManager.flush();
        entityManager.clear();

        // when
        String result = adminStaffService.deactivateStaff(target.getId(), "admin01");
        entityManager.flush();
        entityManager.clear();
        Staff deactivatedStaff = entityManager.find(Staff.class, target.getId());

        // then
        assertThat(result).isEqualTo("Staff deactivated successfully.");
        assertThat(deactivatedStaff.isActive()).isFalse();
    }

    @Test
    @DisplayName("prevents self deactivation")
    void deactivateStaff_selfDeactivate_throwsException() {
        // given
        Department department = persistDepartment("Internal Medicine");
        Staff self = persistStaff("admin01", "A-001", "Admin", StaffRole.ADMIN, department, true);
        entityManager.flush();
        entityManager.clear();

        // when
        // then
        assertThatThrownBy(() -> adminStaffService.deactivateStaff(self.getId(), "admin01"))
                .isInstanceOf(CustomException.class)
                .hasMessage("You cannot deactivate your own account.");
    }

    @Test
    @DisplayName("prevents duplicate deactivate")
    void deactivateStaff_alreadyInactive_throwsException() {
        // given
        Department department = persistDepartment("Internal Medicine");
        Staff inactive = persistStaff("inactive-user", "S-400", "Inactive Staff", StaffRole.STAFF, department, false);
        entityManager.flush();
        entityManager.clear();

        // when
        // then
        assertThatThrownBy(() -> adminStaffService.deactivateStaff(inactive.getId(), "admin01"))
                .isInstanceOf(CustomException.class)
                .hasMessage("Staff is already deactivated.");
    }

    @Test
    @DisplayName("throws when staff is missing")
    void deactivateStaff_notFound_throwsException() {
        // given
        // when
        // then
        assertThatThrownBy(() -> adminStaffService.deactivateStaff(999L, "admin01"))
                .isInstanceOf(CustomException.class)
                .hasMessage("Staff not found.");
    }

    private Department persistDepartment(String name) {
        Department department = Department.create(name, true);
        entityManager.persist(department);
        return department;
    }

    private Staff persistStaff(
            String username,
            String employeeNumber,
            String name,
            StaffRole role,
            Department department,
            boolean active
    ) {
        Staff staff = Staff.create(username, employeeNumber, "{noop}pw", name, role, department);
        if (!active) {
            staff.update(name, department, false);
        }
        entityManager.persist(staff);
        return staff;
    }

    @TestConfiguration
    static class PasswordEncoderTestConfig {

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }
}