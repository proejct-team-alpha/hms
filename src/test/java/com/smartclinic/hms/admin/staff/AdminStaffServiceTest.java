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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

@DataJpaTest
@Import({AdminStaffService.class, AdminStaffServiceTest.PasswordEncoderTestConfig.class})
@TestPropertySource(properties = "spring.sql.init.mode=never")
class AdminStaffServiceTest {

    private static final String INACTIVE_UPDATE_NOT_ALLOWED_MESSAGE = "\uBE44\uD65C\uC131\uD654\uB41C \uC9C1\uC6D0\uC740 \uC218\uC815\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4.";
    private static final String RETIRED_AT_REQUIRED_MESSAGE = "\uD1F4\uC0AC \uC77C\uC2DC\uB294 \uB0A0\uC9DC\uC640 \uC2DC\uAC04\uC744 \uBAA8\uB450 \uC120\uD0DD\uD574\uC57C \uD569\uB2C8\uB2E4.";

    @Autowired
    private AdminStaffService adminStaffService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("getStaffList applies keyword, role, and active filters")
    void getStaffList_appliesKeywordRoleAndEmploymentFilters() {
        // given
        Department internalMedicine = persistDepartment("internal medicine");
        Department surgery = persistDepartment("surgery");

        Staff kimDoctor = persistStaff("kim-doctor", "D-001", "kim doctor", StaffRole.DOCTOR, internalMedicine, true);
        persistStaff("kim-nurse", "N-001", "kim nurse", StaffRole.NURSE, internalMedicine, true);
        Staff leeDoctor = persistStaff("lee-doctor", "D-002", "lee doctor", StaffRole.DOCTOR, surgery, false);
        persistDoctor(kimDoctor, internalMedicine, "MON,WED", "internal medicine");
        persistDoctor(leeDoctor, surgery, "TUE,THU", "surgery");

        entityManager.flush();
        entityManager.clear();

        // when
        AdminStaffListResponse result = adminStaffService.getStaffList(1, 10, "kim", "DOCTOR", "ACTIVE", "admin01");

        // then
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.staffs()).hasSize(1);
        assertThat(result.staffs().getFirst().name()).isEqualTo("kim doctor");
        assertThat(result.staffs().getFirst().role()).isEqualTo("DOCTOR");
        assertThat(result.staffs().getFirst().departmentName()).isEqualTo("internal medicine");
        assertThat(result.staffs().getFirst().active()).isTrue();
    }

    @Test
    @DisplayName("createStaff encrypts password and saves retiredAt")
    void createStaff_encryptsPasswordAndSaves() {
        // given
        LocalDateTime retiredAt = LocalDateTime.now().plusDays(30).withMinute(0).withSecond(0).withNano(0);
        CreateAdminStaffRequest request = new CreateAdminStaffRequest(
                "staff-new",
                "password123",
                "staff new",
                "S-NEW-001",
                "STAFF",
                null,
                true,
                retiredAt,
                List.of()
        );

        // when
        adminStaffService.createStaff(request);
        entityManager.flush();
        entityManager.clear();

        // then
        Staff savedStaff = entityManager.createQuery(
                        "select s from Staff s where s.username = :username", Staff.class)
                .setParameter("username", "staff-new")
                .getSingleResult();

        assertThat(savedStaff.getName()).isEqualTo("staff new");
        assertThat(savedStaff.isActive()).isTrue();
        assertThat(savedStaff.getDepartment()).isNull();
        assertThat(savedStaff.getRetiredAt()).isEqualTo(retiredAt);
        assertThat(savedStaff.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", savedStaff.getPassword())).isTrue();
    }

    @Test
    @DisplayName("createStaff deactivates staff immediately when retiredAt is in the past")
    void createStaff_pastRetiredAt_deactivatesImmediately() {
        // given
        LocalDateTime pastRetiredAt = LocalDateTime.now().minusHours(2).withMinute(0).withSecond(0).withNano(0);
        CreateAdminStaffRequest request = new CreateAdminStaffRequest(
                "retired-staff",
                "password123",
                "retired staff",
                "S-RET-001",
                "STAFF",
                null,
                true,
                pastRetiredAt,
                List.of()
        );

        // when
        adminStaffService.createStaff(request);
        entityManager.flush();
        entityManager.clear();

        // then
        Staff savedStaff = entityManager.createQuery(
                        "select s from Staff s where s.username = :username", Staff.class)
                .setParameter("username", "retired-staff")
                .getSingleResult();

        assertThat(savedStaff.isActive()).isFalse();
        assertThat(savedStaff.getRetiredAt()).isEqualTo(pastRetiredAt);
    }

    @Test
    @DisplayName("createStaff combines retiredAtDate and retiredAtHour with zero minutes")
    void createStaff_combinesRetiredAtDateAndHour_toHourZero() {
        // given
        CreateAdminStaffRequest request = new CreateAdminStaffRequest(
                "hour-user",
                "password123",
                "hour user",
                "S-HOUR-001",
                "STAFF",
                null,
                true,
                null,
                "2026-03-31",
                "18",
                List.of()
        );

        // when
        adminStaffService.createStaff(request);
        entityManager.flush();
        entityManager.clear();

        // then
        Staff savedStaff = entityManager.createQuery(
                        "select s from Staff s where s.username = :username", Staff.class)
                .setParameter("username", "hour-user")
                .getSingleResult();

        assertThat(savedStaff.getRetiredAt()).isEqualTo(LocalDateTime.of(2026, 3, 31, 18, 0));
    }

    @Test
    @DisplayName("createStaff throws validation error when retiredAtDate is provided without retiredAtHour")
    void createStaff_retiredAtDateWithoutHour_throwsException() {
        // given
        CreateAdminStaffRequest request = new CreateAdminStaffRequest(
                "date-only-user",
                "password123",
                "date only user",
                "S-DATE-001",
                "STAFF",
                null,
                true,
                null,
                "2026-03-31",
                null,
                List.of()
        );

        // when
        CustomException ex = catchThrowableOfType(() -> adminStaffService.createStaff(request), CustomException.class);

        // then
        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(ex.getMessage()).isEqualTo(RETIRED_AT_REQUIRED_MESSAGE);
    }

    @Test
    @DisplayName("createStaff creates doctor profile when role is doctor")
    void createStaff_createsDoctorWhenRoleIsDoctor() {
        // given
        Department department = persistDepartment("family medicine");
        CreateAdminStaffRequest request = new CreateAdminStaffRequest(
                "doctor-new",
                "password123",
                "doctor new",
                "D-NEW-001",
                "DOCTOR",
                department.getId(),
                true,
                null,
                List.of("MON", "WED", "FRI")
        );

        // when
        adminStaffService.createStaff(request);
        entityManager.flush();
        entityManager.clear();

        // then
        Staff savedStaff = entityManager.createQuery(
                        "select s from Staff s where s.username = :username", Staff.class)
                .setParameter("username", "doctor-new")
                .getSingleResult();
        Doctor savedDoctor = entityManager.createQuery(
                        "select d from Doctor d join fetch d.staff join fetch d.department where d.staff.id = :staffId", Doctor.class)
                .setParameter("staffId", savedStaff.getId())
                .getSingleResult();

        assertThat(savedStaff.getRole()).isEqualTo(StaffRole.DOCTOR);
        assertThat(savedStaff.getDepartment()).isNull();
        assertThat(savedDoctor.getDepartment().getId()).isEqualTo(department.getId());
        assertThat(savedDoctor.getSpecialty()).isEqualTo("family medicine");
        assertThat(savedDoctor.getAvailableDays()).isEqualTo("MON,WED,FRI");
    }

    @Test
    @DisplayName("createStaff throws conflict for duplicate username")
    void createStaff_duplicateUsername_throwsException() {
        // given
        Department department = persistDepartment("general");
        persistStaff("duplicate-user", "S-001", "existing staff", StaffRole.STAFF, department, true);
        entityManager.flush();
        entityManager.clear();

        CreateAdminStaffRequest request = new CreateAdminStaffRequest(
                "duplicate-user",
                "password123",
                "staff new",
                "S-002",
                "STAFF",
                null,
                true,
                null,
                List.of()
        );

        // when
        CustomException ex = catchThrowableOfType(() -> adminStaffService.createStaff(request), CustomException.class);

        // then
        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo("DUPLICATE_USERNAME");
    }

    @Test
    @DisplayName("updateStaff updates non-doctor fields and ignores department")
    void updateStaff_updatesNameAndActiveAndIgnoresDepartmentForNonDoctor() {
        // given
        Department originalDepartment = persistDepartment("operations");
        Department ignoredDepartment = persistDepartment("internal medicine");
        Staff staff = persistStaff("staff-update", "S-010", "existing staff", StaffRole.STAFF, originalDepartment, true);
        entityManager.flush();
        entityManager.clear();

        UpdateAdminStaffRequest request = new UpdateAdminStaffRequest(
                staff.getId(),
                "updated staff",
                ignoredDepartment.getId(),
                "newpassword123",
                false,
                null,
                List.of()
        );

        // when
        adminStaffService.updateStaff(request);
        entityManager.flush();
        entityManager.clear();

        // then
        Staff updatedStaff = entityManager.find(Staff.class, staff.getId());

        assertThat(updatedStaff.getName()).isEqualTo("updated staff");
        assertThat(updatedStaff.getDepartment()).isNull();
        assertThat(updatedStaff.isActive()).isFalse();
        assertThat(passwordEncoder.matches("newpassword123", updatedStaff.getPassword())).isTrue();
    }

    @Test
    @DisplayName("updateStaff keeps existing password when blank password is submitted")
    void updateStaff_blankPassword_keepsExistingPassword() {
        // given
        Department department = persistDepartment("general-admin");
        Staff staff = persistStaff("staff-keep-password", "S-011", "existing-staff", StaffRole.STAFF, department, true);
        String originalPassword = staff.getPassword();
        entityManager.flush();
        entityManager.clear();

        UpdateAdminStaffRequest request = new UpdateAdminStaffRequest(
                staff.getId(),
                "updated-staff",
                null,
                "",
                true,
                null,
                List.of()
        );

        // when
        adminStaffService.updateStaff(request);
        entityManager.flush();
        entityManager.clear();

        // then
        Staff updatedStaff = entityManager.find(Staff.class, staff.getId());

        assertThat(updatedStaff.getName()).isEqualTo("updated-staff");
        assertThat(updatedStaff.getPassword()).isEqualTo(originalPassword);
        assertThat(updatedStaff.isActive()).isTrue();
    }

    @Test
    @DisplayName("updateStaff updates doctor fields from selected department")
    void updateStaff_updatesDoctorFieldsFromSelectedDepartment() {
        // given
        Department internalMedicine = persistDepartment("internal medicine");
        Department familyMedicine = persistDepartment("family medicine");
        Staff doctorStaff = persistStaff("doctor-user", "D-100", "doctor staff", StaffRole.DOCTOR, internalMedicine, true);
        Doctor doctor = Doctor.create(doctorStaff, internalMedicine, "MON,WED", "internal medicine");
        entityManager.persist(doctor);
        entityManager.flush();
        entityManager.clear();

        UpdateAdminStaffRequest request = new UpdateAdminStaffRequest(
                doctorStaff.getId(),
                "doctor updated",
                familyMedicine.getId(),
                "",
                false,
                null,
                List.of("TUE", "THU", "SAT")
        );

        // when
        adminStaffService.updateStaff(request);
        entityManager.flush();
        entityManager.clear();

        // then
        Staff updatedStaff = entityManager.find(Staff.class, doctorStaff.getId());
        Doctor updatedDoctor = entityManager.createQuery(
                        "select d from Doctor d join fetch d.department where d.staff.id = :staffId", Doctor.class)
                .setParameter("staffId", doctorStaff.getId())
                .getSingleResult();

        assertThat(updatedStaff.getName()).isEqualTo("doctor updated");
        assertThat(updatedStaff.getDepartment()).isNull();
        assertThat(updatedStaff.isActive()).isFalse();
        assertThat(updatedDoctor.getDepartment().getId()).isEqualTo(familyMedicine.getId());
        assertThat(updatedDoctor.getSpecialty()).isEqualTo("family medicine");
        assertThat(updatedDoctor.getAvailableDays()).isEqualTo("TUE,THU,SAT");
    }

    @Test
    @DisplayName("updateStaff deactivates staff immediately when retiredAt is in the past")
    void updateStaff_pastRetiredAt_deactivatesImmediately() {
        // given
        Department department = persistDepartment("internal medicine");
        Staff staff = persistStaff("staff-retire", "S-777", "retire staff", StaffRole.STAFF, department, true);
        LocalDateTime pastRetiredAt = LocalDateTime.now().minusHours(1).withMinute(0).withSecond(0).withNano(0);
        entityManager.flush();
        entityManager.clear();

        UpdateAdminStaffRequest request = new UpdateAdminStaffRequest(
                staff.getId(),
                "retire staff",
                null,
                "",
                true,
                pastRetiredAt,
                List.of()
        );

        // when
        adminStaffService.updateStaff(request, "admin01");
        entityManager.flush();
        entityManager.clear();

        // then
        Staff updatedStaff = entityManager.find(Staff.class, staff.getId());

        assertThat(updatedStaff.isActive()).isFalse();
        assertThat(updatedStaff.getRetiredAt()).isEqualTo(pastRetiredAt);
    }

    @Test
    @DisplayName("updateStaff combines retiredAtDate and retiredAtHour with zero minutes")
    void updateStaff_combinesRetiredAtDateAndHour_toHourZero() {
        // given
        Department department = persistDepartment("internal medicine");
        Staff staff = persistStaff("staff-hour-update", "S-778", "hour update staff", StaffRole.STAFF, department, true);
        entityManager.flush();
        entityManager.clear();

        UpdateAdminStaffRequest request = new UpdateAdminStaffRequest(
                staff.getId(),
                "hour update staff",
                null,
                "",
                true,
                null,
                "2026-04-01",
                "09",
                List.of()
        );

        // when
        adminStaffService.updateStaff(request, "admin01");
        entityManager.flush();
        entityManager.clear();

        // then
        Staff updatedStaff = entityManager.find(Staff.class, staff.getId());

        assertThat(updatedStaff.getRetiredAt()).isEqualTo(LocalDateTime.of(2026, 4, 1, 9, 0));
    }

    @Test
    @DisplayName("updateStaff throws validation error when retiredAtHour is provided without retiredAtDate")
    void updateStaff_retiredAtHourWithoutDate_throwsException() {
        // given
        Department department = persistDepartment("internal medicine");
        Staff staff = persistStaff("staff-hour-only", "S-779", "hour only staff", StaffRole.STAFF, department, true);
        entityManager.flush();
        entityManager.clear();

        UpdateAdminStaffRequest request = new UpdateAdminStaffRequest(
                staff.getId(),
                "hour only staff",
                null,
                "",
                true,
                null,
                null,
                "09",
                List.of()
        );

        // when
        CustomException ex = catchThrowableOfType(() -> adminStaffService.updateStaff(request, "admin01"), CustomException.class);

        // then
        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(ex.getMessage()).isEqualTo(RETIRED_AT_REQUIRED_MESSAGE);
    }

    @Test
    @DisplayName("deactivateStaff sets active to false")
    void deactivateStaff_updatesActiveToFalse() {
        // given
        Department department = persistDepartment("internal medicine");
        Staff target = persistStaff("staff-target", "S-300", "target staff", StaffRole.STAFF, department, true);
        entityManager.flush();
        entityManager.clear();

        // when
        String result = adminStaffService.deactivateStaff(target.getId(), "admin01");
        entityManager.flush();
        entityManager.clear();

        // then
        Staff deactivatedStaff = entityManager.find(Staff.class, target.getId());

        assertThat(result).isNotBlank();
        assertThat(deactivatedStaff.isActive()).isFalse();
        assertThat(deactivatedStaff.getDepartment()).isNull();
    }

    @Test
    @DisplayName("deactivateExpiredStaffs deactivates only expired active staff")
    void deactivateExpiredStaffs_deactivatesOnlyExpiredActiveStaff() {
        // given
        Department department = persistDepartment("internal medicine");
        Staff expired = persistStaff("expired-user", "S-401", "expired staff", StaffRole.STAFF, department, true);
        expired.updateRetiredAt(LocalDateTime.now().minusHours(2).withSecond(0).withNano(0));

        Staff future = persistStaff("future-user", "S-402", "future staff", StaffRole.STAFF, department, true);
        future.updateRetiredAt(LocalDateTime.now().plusHours(2).withSecond(0).withNano(0));

        Staff inactive = persistStaff("inactive-user-2", "S-403", "inactive staff", StaffRole.STAFF, department, false);
        inactive.updateRetiredAt(LocalDateTime.now().minusHours(3).withSecond(0).withNano(0));

        entityManager.flush();
        entityManager.clear();

        // when
        int count = adminStaffService.deactivateExpiredStaffs();
        entityManager.flush();
        entityManager.clear();

        // then
        Staff expiredStaff = entityManager.find(Staff.class, expired.getId());
        Staff futureStaff = entityManager.find(Staff.class, future.getId());
        Staff inactiveStaff = entityManager.find(Staff.class, inactive.getId());

        assertThat(count).isEqualTo(1);
        assertThat(expiredStaff.isActive()).isFalse();
        assertThat(futureStaff.isActive()).isTrue();
        assertThat(inactiveStaff.isActive()).isFalse();
    }

    @Test
    @DisplayName("deactivateStaff blocks self deactivate")
    void deactivateStaff_selfDeactivate_throwsException() {
        // given
        Department department = persistDepartment("internal medicine");
        Staff self = persistStaff("admin01", "A-001", "admin self", StaffRole.ADMIN, department, true);
        entityManager.flush();
        entityManager.clear();

        // when
        CustomException ex = catchThrowableOfType(() -> adminStaffService.deactivateStaff(self.getId(), "admin01"), CustomException.class);

        // then
        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    @DisplayName("updateStaff blocks self deactivate")
    void updateStaff_selfDeactivate_throwsException() {
        // given
        Department department = persistDepartment("internal medicine");
        Staff self = persistStaff("admin01", "A-001", "admin self", StaffRole.ADMIN, department, true);
        entityManager.flush();
        entityManager.clear();

        UpdateAdminStaffRequest request = new UpdateAdminStaffRequest(
                self.getId(),
                "admin self",
                null,
                "",
                false,
                null,
                List.of()
        );

        // when
        CustomException ex = catchThrowableOfType(() -> adminStaffService.updateStaff(request, "admin01"), CustomException.class);

        // then
        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    @DisplayName("updateStaff blocks self retiredAt change")
    void updateStaff_selfRetiredAtChange_throwsException() {
        // given
        Department department = persistDepartment("internal medicine");
        Staff self = persistStaff("admin01", "A-001", "admin self", StaffRole.ADMIN, department, true);
        entityManager.flush();
        entityManager.clear();

        UpdateAdminStaffRequest request = new UpdateAdminStaffRequest(
                self.getId(),
                "admin self",
                null,
                "",
                true,
                LocalDateTime.now().plusDays(7).withSecond(0).withNano(0),
                List.of()
        );

        // when
        CustomException ex = catchThrowableOfType(() -> adminStaffService.updateStaff(request, "admin01"), CustomException.class);

        // then
        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    @DisplayName("updateStaff blocks reactivation for inactive staff")
    void updateStaff_reactivationBlocked_throwsException() {
        // given
        Department department = persistDepartment("general");
        Staff inactive = persistStaff("inactive-reactivation", "S-998", "inactive reactivation", StaffRole.STAFF, department, false);
        entityManager.flush();
        entityManager.clear();

        UpdateAdminStaffRequest request = new UpdateAdminStaffRequest(
                inactive.getId(),
                "inactive reactivation",
                null,
                "",
                true,
                null,
                List.of()
        );

        // when
        CustomException ex = catchThrowableOfType(() -> adminStaffService.updateStaff(request, "manager01"), CustomException.class);

        // then
        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    @DisplayName("updateStaff blocks inactive staff update")
    void updateStaff_inactiveStaffUpdateBlocked_throwsException() {
        // given
        Department department = persistDepartment("general");
        Staff inactive = persistStaff("inactive-user", "S-999", "inactive staff", StaffRole.STAFF, department, false);
        entityManager.flush();
        entityManager.clear();

        UpdateAdminStaffRequest request = new UpdateAdminStaffRequest(
                inactive.getId(),
                "inactive staff",
                null,
                "",
                false,
                null,
                List.of()
        );

        // when
        CustomException ex = catchThrowableOfType(() -> adminStaffService.updateStaff(request, "admin01"), CustomException.class);

        // then
        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(ex.getMessage()).isEqualTo(INACTIVE_UPDATE_NOT_ALLOWED_MESSAGE);
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

    private Doctor persistDoctor(Staff staff, Department department, String availableDays, String specialty) {
        Doctor doctor = Doctor.create(staff, department, availableDays, specialty);
        entityManager.persist(doctor);
        return doctor;
    }

    @TestConfiguration
    static class PasswordEncoderTestConfig {

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }
}
