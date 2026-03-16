package com.smartclinic.hms.admin.staff;

import com.smartclinic.hms.admin.staff.dto.AdminStaffListResponse;
import com.smartclinic.hms.domain.Department;
import com.smartclinic.hms.domain.Staff;
import com.smartclinic.hms.domain.StaffRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(AdminStaffService.class)
@TestPropertySource(properties = "spring.sql.init.mode=never")
class AdminStaffServiceTest {

    @Autowired
    private AdminStaffService adminStaffService;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("키워드, 역할, 재직 상태 필터를 함께 적용한다")
    void getStaffList_appliesKeywordRoleAndEmploymentFilters() {
        // given
        Department internalMedicine = persistDepartment("내과");
        Department surgery = persistDepartment("외과");

        persistStaff("kim-doctor", "D-001", "김의사", StaffRole.DOCTOR, internalMedicine, true);
        persistStaff("kim-nurse", "N-001", "김간호", StaffRole.NURSE, internalMedicine, true);
        persistStaff("lee-doctor", "D-002", "이의사", StaffRole.DOCTOR, surgery, false);

        entityManager.flush();
        entityManager.clear();

        // when
        AdminStaffListResponse result = adminStaffService.getStaffList(1, 10, "kim", "DOCTOR", "ACTIVE");

        // then
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.staffs()).hasSize(1);
        assertThat(result.staffs().getFirst().name()).isEqualTo("김의사");
        assertThat(result.staffs().getFirst().role()).isEqualTo("DOCTOR");
        assertThat(result.staffs().getFirst().active()).isTrue();
    }

    @Test
    @DisplayName("기본 페이징 크기와 정렬을 적용한다")
    void getStaffList_appliesDefaultPagingAndSort() {
        // given
        Department department = persistDepartment("운영지원");

        for (int i = 1; i <= 12; i++) {
            persistStaff(
                    "staff" + i,
                    "S-" + i,
                    "직원" + i,
                    StaffRole.STAFF,
                    department,
                    true
            );
        }

        entityManager.flush();
        entityManager.clear();

        // when
        AdminStaffListResponse result = adminStaffService.getStaffList(1, 10, null, "ALL", "ALL");

        // then
        assertThat(result.currentPage()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalCount()).isEqualTo(12);
        assertThat(result.staffs()).hasSize(10);
        assertThat(result.staffs().getFirst().employeeNumber()).isEqualTo("S-12");
    }

    private Department persistDepartment(String name) {
        Department department = Department.create(name, true);
        entityManager.persist(department);
        return department;
    }

    private void persistStaff(
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
    }
}
