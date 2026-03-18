package com.smartclinic.hms.admin.department;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;

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

import com.smartclinic.hms.domain.Department;

@ExtendWith(MockitoExtension.class)
class AdminDepartmentServiceTest {

    @Mock
    private AdminDepartmentRepository adminDepartmentRepository;

    @InjectMocks
    private AdminDepartmentService adminDepartmentService;

    @Test
    @DisplayName("잘못된 페이지 파라미터가 들어오면 기본값으로 보정한다")
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
    @DisplayName("진료과 목록 조회는 페이지 정보와 링크를 함께 계산한다")
    void getDepartmentList_buildsPagedResponse() {
        // given
        Department internalMedicine = Department.create("내과", true);
        Department surgery = Department.create("외과", false);
        PageImpl<Department> pageResult = new PageImpl<>(
                List.of(internalMedicine, surgery),
                PageRequest.of(1, 5),
                12
        );
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
    @DisplayName("진료과 목록 조회는 데이터가 없으면 빈 목록 상태를 반환한다")
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
    @DisplayName("진료과 목록 조회는 각 페이지 번호와 링크를 일관되게 계산한다")
    void getDepartmentList_buildsPageLinksWithExpectedUrls() {
        // given
        Department internalMedicine = Department.create("내과", true);
        PageImpl<Department> pageResult = new PageImpl<>(
                List.of(internalMedicine),
                PageRequest.of(2, 5),
                16
        );
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
                        "/admin/department/list?page=4&size=5"
                );
        assertThat(result.pageLinks())
                .extracting(AdminDepartmentPageLinkResponse::active)
                .containsExactly(false, false, true, false);
    }

    @Test
    @DisplayName("진료과 등록은 화면에서 전달한 active 값을 그대로 저장한다")
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
