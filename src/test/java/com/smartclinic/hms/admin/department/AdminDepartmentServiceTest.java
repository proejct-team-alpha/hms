package com.smartclinic.hms.admin.department;

import com.smartclinic.hms.domain.Department;
import com.smartclinic.hms.reservation.reservation.DepartmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AdminDepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private AdminDepartmentService adminDepartmentService;

    // ── getDepartmentList ────────────────────────────────────────────────────

    @Test
    @DisplayName("getDepartmentList — 전체 진료과 목록을 DTO로 변환하여 반환")
    void getDepartmentList_returnsMappedDtos() {
        Department dept1 = Department.create("내과", true);
        Department dept2 = Department.create("외과", false);
        given(departmentRepository.findAll()).willReturn(List.of(dept1, dept2));

        List<AdminDepartmentDto> result = adminDepartmentService.getDepartmentList();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("내과");
        assertThat(result.get(0).isActive()).isTrue();
        assertThat(result.get(1).getName()).isEqualTo("외과");
        assertThat(result.get(1).isActive()).isFalse();
    }

    @Test
    @DisplayName("getDepartmentList — 진료과가 없으면 빈 목록 반환")
    void getDepartmentList_withNoDepartments_returnsEmpty() {
        given(departmentRepository.findAll()).willReturn(List.of());

        List<AdminDepartmentDto> result = adminDepartmentService.getDepartmentList();

        assertThat(result).isEmpty();
    }

    // ── createDepartment ─────────────────────────────────────────────────────

    @Test
    @DisplayName("createDepartment — 활성 상태의 진료과를 저장")
    void createDepartment_savesActiveDepartment() {
        adminDepartmentService.createDepartment("신경과");

        then(departmentRepository).should().save(any(Department.class));
    }

    @Test
    @DisplayName("AdminDepartmentDto — 활성 진료과의 배지 텍스트 확인")
    void adminDepartmentDto_activeDepartment_showsCorrectText() {
        Department dept = Department.create("피부과", true);

        AdminDepartmentDto dto = new AdminDepartmentDto(dept);

        assertThat(dto.getActiveText()).isEqualTo("운영 중");
    }

    @Test
    @DisplayName("AdminDepartmentDto — 비활성 진료과의 배지 텍스트 확인")
    void adminDepartmentDto_inactiveDepartment_showsCorrectText() {
        Department dept = Department.create("폐업과", false);

        AdminDepartmentDto dto = new AdminDepartmentDto(dept);

        assertThat(dto.getActiveText()).isEqualTo("비운영");
    }
}
