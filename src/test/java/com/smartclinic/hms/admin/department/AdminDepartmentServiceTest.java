package com.smartclinic.hms.admin.department;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.smartclinic.hms.common.exception.CustomException;
import com.smartclinic.hms.domain.Department;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminDepartmentServiceTest {

    @Mock
    private AdminDepartmentRepository adminDepartmentRepository;

    @InjectMocks
    private AdminDepartmentService adminDepartmentService;

    @Test
    @DisplayName("getDepartmentList uses defaults when params are invalid")
    void getDepartmentList_usesDefaultPageAndSizeWhenInvalid() {
        // given
        given(adminDepartmentRepository.findAllByOrderByIdDesc(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        // when
        AdminDepartmentListResponse result = adminDepartmentService.getDepartmentList(0, 0);

        // then
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(adminDepartmentRepository).should().findAllByOrderByIdDesc(pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().isUnsorted()).isTrue();
        assertThat(result.currentPage()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalPages()).isEqualTo(0);
        assertThat(result.hasPages()).isFalse();
        assertThat(result.pageLinks()).isEmpty();
    }

    @Test
    @DisplayName("getDepartmentList builds paged response")
    void getDepartmentList_buildsPagedResponse() {
        // given
        Department internalMedicine = Department.create("내과", true);
        Department surgery = Department.create("외과", false);
        PageImpl<Department> pageResult = new PageImpl<>(
                List.of(internalMedicine, surgery),
                PageRequest.of(1, 5),
                12);
        given(adminDepartmentRepository.findAllByOrderByIdDesc(any(Pageable.class)))
                .willReturn(pageResult);

        // when
        AdminDepartmentListResponse result = adminDepartmentService.getDepartmentList(2, 5);

        // then
        assertThat(result.departments()).hasSize(2);
        assertThat(result.departments().get(0).getName()).isEqualTo("내과");
        assertThat(result.departments().get(0).getActiveText()).isEqualTo("운영 중");
        assertThat(result.departments().get(1).getName()).isEqualTo("외과");
        assertThat(result.departments().get(1).getActiveText()).isEqualTo("비운영");
        assertThat(result.totalCount()).isEqualTo(12);
        assertThat(result.currentPage()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.hasPages()).isTrue();
        assertThat(result.hasPrevious()).isTrue();
        assertThat(result.hasNext()).isTrue();
        assertThat(result.previousUrl()).isEqualTo("/admin/department/list?page=1&size=5");
        assertThat(result.nextUrl()).isEqualTo("/admin/department/list?page=3&size=5");
        assertThat(result.pageLinks()).hasSize(3);
        assertThat(result.pageLinks().get(1).active()).isTrue();
    }

    @Test
    @DisplayName("getDepartmentList returns empty state when no data exists")
    void getDepartmentList_returnsEmptyStateWhenNoDepartments() {
        // given
        given(adminDepartmentRepository.findAllByOrderByIdDesc(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        // when
        AdminDepartmentListResponse result = adminDepartmentService.getDepartmentList(1, 10);

        // then
        assertThat(result.departments()).isEmpty();
        assertThat(result.totalCount()).isZero();
        assertThat(result.currentPage()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(0);
        assertThat(result.hasPages()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.previousUrl()).isEmpty();
        assertThat(result.nextUrl()).isEmpty();
        assertThat(result.pageLinks()).isEmpty();
    }

    @Test
    @DisplayName("getDepartmentList builds page links with expected urls")
    void getDepartmentList_buildsPageLinksWithExpectedUrls() {
        // given
        Department internalMedicine = Department.create("내과", true);
        PageImpl<Department> pageResult = new PageImpl<>(
                List.of(internalMedicine),
                PageRequest.of(2, 5),
                16);
        given(adminDepartmentRepository.findAllByOrderByIdDesc(any(Pageable.class)))
                .willReturn(pageResult);

        // when
        AdminDepartmentListResponse result = adminDepartmentService.getDepartmentList(3, 5);

        // then
        assertThat(result.currentPage()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(4);
        assertThat(result.hasPages()).isTrue();
        assertThat(result.previousUrl()).isEqualTo("/admin/department/list?page=2&size=5");
        assertThat(result.nextUrl()).isEqualTo("/admin/department/list?page=4&size=5");
        assertThat(result.pageLinks())
                .extracting(AdminDepartmentPageLinkResponse::page)
                .containsExactly(1, 2, 3, 4);
        assertThat(result.pageLinks())
                .extracting(AdminDepartmentPageLinkResponse::url)
                .containsExactly(
                        "/admin/department/list?page=1&size=5",
                        "/admin/department/list?page=2&size=5",
                        "/admin/department/list?page=3&size=5",
                        "/admin/department/list?page=4&size=5");
        assertThat(result.pageLinks())
                .extracting(AdminDepartmentPageLinkResponse::active)
                .containsExactly(false, false, true, false);
    }

    @Test
    @DisplayName("getDepartmentDetail maps detail response")
    void getDepartmentDetail_mapsDetailResponse() {
        // given
        Department department = Department.create("내과", true);
        ReflectionTestUtils.setField(department, "id", 9L);
        given(adminDepartmentRepository.findById(9L)).willReturn(Optional.of(department));

        // when
        AdminDepartmentDetailResponse result = adminDepartmentService.getDepartmentDetail(9L);

        // then
        assertThat(result.departmentId()).isEqualTo(9L);
        assertThat(result.name()).isEqualTo("내과");
        assertThat(result.active()).isTrue();
        assertThat(result.activeText()).isEqualTo("운영 중");
        assertThat(result.deactivatable()).isTrue();
        assertThat(result.activatable()).isFalse();
        assertThat(result.updateAction()).isEqualTo("/admin/department/update");
    }

    @Test
    @DisplayName("getDepartmentDetail throws when department is missing")
    void getDepartmentDetail_throwsWhenDepartmentMissing() {
        // given
        given(adminDepartmentRepository.findById(99L)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> adminDepartmentService.getDepartmentDetail(99L))
                .isInstanceOf(CustomException.class)
                .hasMessage("진료과를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("updateDepartmentName renames target department")
    void updateDepartmentName_renamesDepartment() {
        // given
        Department department = Department.create("기존 이름", true);
        ReflectionTestUtils.setField(department, "id", 4L);
        given(adminDepartmentRepository.findById(4L)).willReturn(Optional.of(department));
        given(adminDepartmentRepository.existsByNameIgnoreCaseAndIdNot("새 이름", 4L)).willReturn(false);

        // when
        String result = adminDepartmentService.updateDepartmentName(4L, "  새 이름  ");

        // then
        assertThat(result).isEqualTo("진료과명이 수정되었습니다.");
        assertThat(department.getName()).isEqualTo("새 이름");
        assertThat(department.isActive()).isTrue();
        then(adminDepartmentRepository).should().save(department);
    }

    @Test
    @DisplayName("updateDepartmentName rejects blank name")
    void updateDepartmentName_throwsWhenNameBlank() {
        // given

        // when
        // then
        assertThatThrownBy(() -> adminDepartmentService.updateDepartmentName(4L, "   "))
                .isInstanceOf(CustomException.class)
                .hasMessage("진료과명은 필수입니다.");
    }

    @Test
    @DisplayName("updateDepartmentName rejects duplicate name")
    void updateDepartmentName_throwsWhenNameDuplicated() {
        // given
        Department department = Department.create("기존 이름", true);
        ReflectionTestUtils.setField(department, "id", 4L);
        given(adminDepartmentRepository.findById(4L)).willReturn(Optional.of(department));
        given(adminDepartmentRepository.existsByNameIgnoreCaseAndIdNot("내과", 4L)).willReturn(true);

        // when
        // then
        assertThatThrownBy(() -> adminDepartmentService.updateDepartmentName(4L, "내과"))
                .isInstanceOf(CustomException.class)
                .hasMessage("이미 존재하는 진료과명입니다.");
    }

    @Test
    @DisplayName("updateDepartmentName rejects missing department")
    void updateDepartmentName_throwsWhenDepartmentMissing() {
        // given
        given(adminDepartmentRepository.findById(44L)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> adminDepartmentService.updateDepartmentName(44L, "새 이름"))
                .isInstanceOf(CustomException.class)
                .hasMessage("진료과를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("deactivateDepartment turns active flag off")
    void deactivateDepartment_deactivatesDepartment() {
        // given
        Department department = Department.create("내과", true);
        ReflectionTestUtils.setField(department, "id", 8L);
        given(adminDepartmentRepository.findById(8L)).willReturn(Optional.of(department));

        // when
        String result = adminDepartmentService.deactivateDepartment(8L);

        // then
        assertThat(result).isEqualTo("진료과가 비활성화되었습니다.");
        assertThat(department.isActive()).isFalse();
        then(adminDepartmentRepository).should().save(department);
    }

    @Test
    @DisplayName("deactivateDepartment rejects already inactive department")
    void deactivateDepartment_throwsWhenAlreadyInactive() {
        // given
        Department department = Department.create("외과", false);
        ReflectionTestUtils.setField(department, "id", 8L);
        given(adminDepartmentRepository.findById(8L)).willReturn(Optional.of(department));

        // when
        // then
        assertThatThrownBy(() -> adminDepartmentService.deactivateDepartment(8L))
                .isInstanceOf(CustomException.class)
                .hasMessage("이미 비활성화된 진료과입니다.");
    }

    @Test
    @DisplayName("deactivateDepartment rejects missing department")
    void deactivateDepartment_throwsWhenDepartmentMissing() {
        // given
        given(adminDepartmentRepository.findById(81L)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> adminDepartmentService.deactivateDepartment(81L))
                .isInstanceOf(CustomException.class)
                .hasMessage("진료과를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("activateDepartment turns active flag on")
    void activateDepartment_activatesDepartment() {
        // given
        Department department = Department.create("외과", false);
        ReflectionTestUtils.setField(department, "id", 9L);
        given(adminDepartmentRepository.findById(9L)).willReturn(Optional.of(department));

        // when
        String result = adminDepartmentService.activateDepartment(9L);

        // then
        assertThat(result).isEqualTo("진료과가 활성화되었습니다.");
        assertThat(department.isActive()).isTrue();
        then(adminDepartmentRepository).should().save(department);
    }

    @Test
    @DisplayName("activateDepartment rejects already active department")
    void activateDepartment_throwsWhenAlreadyActive() {
        // given
        Department department = Department.create("내과", true);
        ReflectionTestUtils.setField(department, "id", 9L);
        given(adminDepartmentRepository.findById(9L)).willReturn(Optional.of(department));

        // when
        // then
        assertThatThrownBy(() -> adminDepartmentService.activateDepartment(9L))
                .isInstanceOf(CustomException.class)
                .hasMessage("이미 활성화된 진료과입니다.");
    }

    @Test
    @DisplayName("activateDepartment rejects missing department")
    void activateDepartment_throwsWhenDepartmentMissing() {
        // given
        given(adminDepartmentRepository.findById(91L)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> adminDepartmentService.activateDepartment(91L))
                .isInstanceOf(CustomException.class)
                .hasMessage("진료과를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("createDepartment rejects duplicate name")
    void createDepartment_throwsWhenNameDuplicated() {
        // given
        given(adminDepartmentRepository.existsByNameIgnoreCase("내과")).willReturn(true);

        // when
        // then
        assertThatThrownBy(() -> adminDepartmentService.createDepartment("내과", true))
                .isInstanceOf(CustomException.class)
                .hasMessage("이미 존재하는 진료과명입니다.");
    }

    @Test
    @DisplayName("createDepartment stores active flag from request")
    void createDepartment_savesActiveValueFromRequest() {
        // given

        // when
        adminDepartmentService.createDepartment("내과", false);

        // then
        ArgumentCaptor<Department> departmentCaptor = ArgumentCaptor.forClass(Department.class);
        then(adminDepartmentRepository).should().save(departmentCaptor.capture());

        Department savedDepartment = departmentCaptor.getValue();
        assertThat(savedDepartment.getName()).isEqualTo("내과");
        assertThat(savedDepartment.isActive()).isFalse();
    }
}
