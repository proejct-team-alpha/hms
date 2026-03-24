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
    @DisplayName("이름 역할 재직 상태 필터를 조합 적용한다")
    void getStaffList_appliesKeywordRoleAndEmploymentFilters() {
        Department internalMedicine = persistDepartment("내과");
        Department surgery = persistDepartment("외과");

        Staff kimDoctor = persistStaff("kim-doctor", "D-001", "김의사", StaffRole.DOCTOR, internalMedicine, true);
        persistStaff("kim-nurse", "N-001", "김간호", StaffRole.NURSE, internalMedicine, true);
        Staff leeDoctor = persistStaff("lee-doctor", "D-002", "이의사", StaffRole.DOCTOR, surgery, false);
        persistDoctor(kimDoctor, internalMedicine, "MON,WED", "내과");
        persistDoctor(leeDoctor, surgery, "TUE,THU", "외과");

        entityManager.flush();
        entityManager.clear();

        AdminStaffListResponse result = adminStaffService.getStaffList(1, 10, "kim", "DOCTOR", "ACTIVE", "admin01");

        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.staffs()).hasSize(1);
        assertThat(result.staffs().getFirst().name()).isEqualTo("김의사");
        assertThat(result.staffs().getFirst().role()).isEqualTo("DOCTOR");
        assertThat(result.staffs().getFirst().departmentName()).isEqualTo("내과");
        assertThat(result.staffs().getFirst().active()).isTrue();
        assertThat(result.staffs().getFirst().deactivatable()).isTrue();
    }

    @Test
    @DisplayName("직원 등록 시 비밀번호를 BCrypt로 암호화해 저장한다")
    void createStaff_encryptsPasswordAndSaves() {
        CreateAdminStaffRequest request = new CreateAdminStaffRequest(
                "staff-new",
                "password123",
                "신규직원",
                "S-NEW-001",
                "STAFF",
                null,
                true,
                List.of()
        );

        adminStaffService.createStaff(request);
        entityManager.flush();
        entityManager.clear();

        Staff savedStaff = entityManager.createQuery(
                        "select s from Staff s where s.username = :username", Staff.class)
                .setParameter("username", "staff-new")
                .getSingleResult();

        assertThat(savedStaff.getName()).isEqualTo("신규직원");
        assertThat(savedStaff.isActive()).isTrue();
        assertThat(savedStaff.getDepartment()).isNull();
        assertThat(savedStaff.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", savedStaff.getPassword())).isTrue();
    }

    @Test
    @DisplayName("의사 역할로 등록하면 Doctor 엔티티를 함께 생성한다")
    void createStaff_createsDoctorWhenRoleIsDoctor() {
        Department department = persistDepartment("가정의학과");
        CreateAdminStaffRequest request = new CreateAdminStaffRequest(
                "doctor-new",
                "password123",
                "신규의사",
                "D-NEW-001",
                "DOCTOR",
                department.getId(),
                true,
                List.of("MON", "WED", "FRI")
        );

        adminStaffService.createStaff(request);
        entityManager.flush();
        entityManager.clear();

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
        assertThat(savedDoctor.getStaff().getId()).isEqualTo(savedStaff.getId());
        assertThat(savedDoctor.getDepartment().getId()).isEqualTo(department.getId());
        assertThat(savedDoctor.getSpecialty()).isEqualTo("가정의학과");
        assertThat(savedDoctor.getAvailableDays()).isEqualTo("MON,WED,FRI");
    }

    @Test
    @DisplayName("중복 로그인 아이디로 직원 등록 시 예외가 발생한다")
    void createStaff_duplicateUsername_throwsException() {
        Department department = persistDepartment("외과");
        persistStaff("duplicate-user", "S-001", "기존직원", StaffRole.STAFF, department, true);
        entityManager.flush();
        entityManager.clear();

        CreateAdminStaffRequest request = new CreateAdminStaffRequest(
                "duplicate-user",
                "password123",
                "신규직원",
                "S-002",
                "STAFF",
                null,
                true,
                List.of()
        );

        assertThatThrownBy(() -> adminStaffService.createStaff(request))
                .isInstanceOf(CustomException.class)
                .hasMessage("이미 사용 중인 로그인 아이디입니다.");
    }

    @Test
    @DisplayName("중복 사번으로 직원 등록 시 예외가 발생한다")
    void createStaff_duplicateEmployeeNumber_throwsException() {
        Department department = persistDepartment("정형외과");
        persistStaff("existing-user", "S-001", "기존직원", StaffRole.STAFF, department, true);
        entityManager.flush();
        entityManager.clear();

        CreateAdminStaffRequest request = new CreateAdminStaffRequest(
                "new-user",
                "password123",
                "신규직원",
                "S-001",
                "STAFF",
                null,
                true,
                List.of()
        );

        assertThatThrownBy(() -> adminStaffService.createStaff(request))
                .isInstanceOf(CustomException.class)
                .hasMessage("이미 사용 중인 사번입니다.");
    }

    @Test
    @DisplayName("비의사 수정 시 전달된 부서 값은 무시하고 재직 상태만 반영한다")
    void updateStaff_updatesNameAndActiveAndIgnoresDepartmentForNonDoctor() {
        Department originalDepartment = persistDepartment("총무과");
        Department ignoredDepartment = persistDepartment("내과");
        Staff staff = persistStaff("staff-update", "S-010", "기존직원", StaffRole.STAFF, originalDepartment, true);
        entityManager.flush();
        entityManager.clear();

        UpdateAdminStaffRequest request = new UpdateAdminStaffRequest(
                staff.getId(),
                "수정직원",
                ignoredDepartment.getId(),
                "newpassword123",
                false,
                List.of()
        );

        adminStaffService.updateStaff(request);
        entityManager.flush();
        entityManager.clear();

        Staff updatedStaff = entityManager.find(Staff.class, staff.getId());

        assertThat(updatedStaff.getName()).isEqualTo("수정직원");
        assertThat(updatedStaff.getDepartment()).isNull();
        assertThat(updatedStaff.isActive()).isFalse();
        assertThat(passwordEncoder.matches("newpassword123", updatedStaff.getPassword())).isTrue();
    }

    @Test
    @DisplayName("직원 수정 시 비밀번호가 비어 있으면 기존 비밀번호를 유지한다")
    void updateStaff_blankPassword_keepsExistingPassword() {
        Department originalDepartment = persistDepartment("general-admin");
        Staff staff = persistStaff("staff-keep-password", "S-011", "existing-staff", StaffRole.STAFF, originalDepartment, true);
        String originalPassword = staff.getPassword();
        entityManager.flush();
        entityManager.clear();

        UpdateAdminStaffRequest request = new UpdateAdminStaffRequest(
                staff.getId(),
                "updated-staff",
                null,
                "",
                true,
                List.of()
        );

        adminStaffService.updateStaff(request);
        entityManager.flush();
        entityManager.clear();

        Staff updatedStaff = entityManager.find(Staff.class, staff.getId());

        assertThat(updatedStaff.getName()).isEqualTo("updated-staff");
        assertThat(updatedStaff.getPassword()).isEqualTo(originalPassword);
        assertThat(updatedStaff.isActive()).isTrue();
    }

    @Test
    @DisplayName("의사 수정 시 선택한 부서를 기준으로 의사 정보를 갱신한다")
    void updateStaff_updatesDoctorFieldsFromSelectedDepartment() {
        Department internalMedicine = persistDepartment("내과");
        Department familyMedicine = persistDepartment("가정의학과");
        Staff doctorStaff = persistStaff("doctor-user", "D-100", "김의사", StaffRole.DOCTOR, internalMedicine, true);
        Doctor doctor = Doctor.create(doctorStaff, internalMedicine, "MON,WED", "소화기내과");
        entityManager.persist(doctor);
        entityManager.flush();
        entityManager.clear();

        UpdateAdminStaffRequest request = new UpdateAdminStaffRequest(
                doctorStaff.getId(),
                "김가정의학의",
                familyMedicine.getId(),
                "",
                false,
                List.of("TUE", "THU", "SAT")
        );

        adminStaffService.updateStaff(request);
        entityManager.flush();
        entityManager.clear();

        Staff updatedStaff = entityManager.find(Staff.class, doctorStaff.getId());
        Doctor updatedDoctor = entityManager.createQuery(
                        "select d from Doctor d join fetch d.department where d.staff.id = :staffId", Doctor.class)
                .setParameter("staffId", doctorStaff.getId())
                .getSingleResult();

        assertThat(updatedStaff.getName()).isEqualTo("김가정의학의");
        assertThat(updatedStaff.getDepartment()).isNull();
        assertThat(updatedStaff.isActive()).isFalse();
        assertThat(updatedDoctor.getDepartment().getId()).isEqualTo(familyMedicine.getId());
        assertThat(updatedDoctor.getSpecialty()).isEqualTo("가정의학과");
        assertThat(updatedDoctor.getAvailableDays()).isEqualTo("TUE,THU,SAT");
    }

    @Test
    @DisplayName("직원 비활성화 시 active 값을 false로 변경한다")
    void deactivateStaff_updatesActiveToFalse() {
        Department department = persistDepartment("내과");
        Staff target = persistStaff("staff-target", "S-300", "대상직원", StaffRole.STAFF, department, true);
        entityManager.flush();
        entityManager.clear();

        String result = adminStaffService.deactivateStaff(target.getId(), "admin01");
        entityManager.flush();
        entityManager.clear();
        Staff deactivatedStaff = entityManager.find(Staff.class, target.getId());

        assertThat(result).isNotBlank();
        assertThat(deactivatedStaff.isActive()).isFalse();
        assertThat(deactivatedStaff.getDepartment()).isNull();
    }

    @Test
    @DisplayName("본인 계정은 비활성화할 수 없다")
    void deactivateStaff_selfDeactivate_throwsException() {
        Department department = persistDepartment("내과");
        Staff self = persistStaff("admin01", "A-001", "관리자", StaffRole.ADMIN, department, true);
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> adminStaffService.deactivateStaff(self.getId(), "admin01"))
                .isInstanceOf(CustomException.class)
                .hasMessage("본인 계정은 비활성화할 수 없습니다.");
    }

    @Test
    @DisplayName("이미 비활성화된 직원은 다시 비활성화할 수 없다")
    void deactivateStaff_alreadyInactive_throwsException() {
        Department department = persistDepartment("내과");
        Staff inactive = persistStaff("inactive-user", "S-400", "비활성직원", StaffRole.STAFF, department, false);
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> adminStaffService.deactivateStaff(inactive.getId(), "admin01"))
                .isInstanceOf(CustomException.class)
                .hasMessage("이미 비활성화된 직원입니다.");
    }

    @Test
    @DisplayName("존재하지 않는 직원은 비활성화할 수 없다")
    void deactivateStaff_notFound_throwsException() {
        assertThatThrownBy(() -> adminStaffService.deactivateStaff(999L, "admin01"))
                .isInstanceOf(CustomException.class)
                .hasMessage("직원을 찾을 수 없습니다.");
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