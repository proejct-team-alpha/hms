package com.smartclinic.hms.admin.department;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.smartclinic.hms.domain.Department;

class AdminDepartmentDetailResponseTest {

    @Test
    @DisplayName("운영 중 진료과는 비활성화 버튼만 노출하는 상세 응답을 생성한다")
    void from_buildsResponseForActiveDepartment() {
        // given
        Department department = Department.create("내과", true);
        ReflectionTestUtils.setField(department, "id", 3L);

        // when
        AdminDepartmentDetailResponse result = AdminDepartmentDetailResponse.from(department);

        // then
        assertThat(result.departmentId()).isEqualTo(3L);
        assertThat(result.name()).isEqualTo("내과");
        assertThat(result.active()).isTrue();
        assertThat(result.activeText()).isEqualTo("운영 중");
        assertThat(result.activeBadgeClass()).contains("bg-green-100");
        assertThat(result.activatable()).isFalse();
        assertThat(result.deactivatable()).isTrue();
        assertThat(result.updateAction()).isEqualTo("/admin/department/update");
        assertThat(result.activateAction()).isEqualTo("/admin/department/activate");
        assertThat(result.deactivateAction()).isEqualTo("/admin/department/deactivate");
    }

    @Test
    @DisplayName("비운영 진료과는 활성화 버튼만 노출하는 상세 응답을 생성한다")
    void from_buildsResponseForInactiveDepartment() {
        // given
        Department department = Department.create("외과", false);
        ReflectionTestUtils.setField(department, "id", 7L);

        // when
        AdminDepartmentDetailResponse result = AdminDepartmentDetailResponse.from(department);

        // then
        assertThat(result.departmentId()).isEqualTo(7L);
        assertThat(result.name()).isEqualTo("외과");
        assertThat(result.active()).isFalse();
        assertThat(result.activeText()).isEqualTo("비운영");
        assertThat(result.activeBadgeClass()).contains("bg-red-100");
        assertThat(result.activatable()).isTrue();
        assertThat(result.deactivatable()).isFalse();
        assertThat(result.updateAction()).isEqualTo("/admin/department/update");
        assertThat(result.activateAction()).isEqualTo("/admin/department/activate");
        assertThat(result.deactivateAction()).isEqualTo("/admin/department/deactivate");
    }
}
