package com.smartclinic.hms.admin.department;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smartclinic.hms.domain.Department;

@ExtendWith(MockitoExtension.class)
class AdminDepartmentServiceTest {

    @Mock
    private AdminDepartmentRepository adminDepartmentRepository;

    @InjectMocks
    private AdminDepartmentService adminDepartmentService;

    @Test
    @DisplayName("진료과 목록 조회는 admin.department 리포지토리 기준으로 정렬된 결과를 사용한다")
    void getDepartmentList_usesAdminDepartmentRepository() {
        // given
        Department surgery = Department.create("외과", false);
        Department internalMedicine = Department.create("내과", true);
        given(adminDepartmentRepository.findAllByOrderByNameAsc())
                .willReturn(List.of(internalMedicine, surgery));

        // when
        List<AdminDepartmentDto> result = adminDepartmentService.getDepartmentList();

        // then
        then(adminDepartmentRepository).should().findAllByOrderByNameAsc();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("내과");
        assertThat(result.get(0).getActiveText()).isEqualTo("운영 중");
        assertThat(result.get(1).getName()).isEqualTo("외과");
        assertThat(result.get(1).getActiveText()).isEqualTo("비운영");
    }
}
