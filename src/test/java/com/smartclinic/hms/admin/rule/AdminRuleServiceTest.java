package com.smartclinic.hms.admin.rule;

import com.smartclinic.hms.domain.HospitalRule;
import com.smartclinic.hms.domain.HospitalRuleCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AdminRuleServiceTest {

    @Mock
    private HospitalRuleRepository hospitalRuleRepository;

    @InjectMocks
    private AdminRuleService adminRuleService;

    @Test
    @DisplayName("getRuleList returns mapped dtos from full list")
    void getRuleList_returnsMappedDtos() {
        // given
        HospitalRule rule1 = HospitalRule.create("응급 지침", "응급실 우선 대응 안내", HospitalRuleCategory.EMERGENCY);
        HospitalRule rule2 = HospitalRule.create("물품 관리", "물품 정리 방법", HospitalRuleCategory.SUPPLY);
        given(hospitalRuleRepository.findAllByOrderByCreatedAtDesc()).willReturn(List.of(rule1, rule2));

        // when
        List<AdminRuleDto> result = adminRuleService.getRuleList();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("응급 지침");
        assertThat(result.get(0).getCategoryText()).isEqualTo("응급");
        assertThat(result.get(1).getTitle()).isEqualTo("물품 관리");
        assertThat(result.get(1).getCategoryText()).isEqualTo("물품");
    }

    @Test
    @DisplayName("getRuleList returns empty list when no rule exists")
    void getRuleList_withNoRules_returnsEmpty() {
        // given
        given(hospitalRuleRepository.findAllByOrderByCreatedAtDesc()).willReturn(List.of());

        // when
        List<AdminRuleDto> result = adminRuleService.getRuleList();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("filtered rule list normalizes params and returns paging metadata")
    void getRuleList_withFilters_returnsPagedResponse() {
        // given
        HospitalRule rule = HospitalRule.create("야간 근무 지침", "야간 근무 인수인계 규칙", HospitalRuleCategory.DUTY);
        var page = new PageImpl<>(List.of(rule), PageRequest.of(1, 5), 6);
        given(hospitalRuleRepository.search(
                eq(HospitalRuleCategory.DUTY),
                eq(Boolean.TRUE),
                eq("night"),
                any(PageRequest.class)
        )).willReturn(page);

        // when
        AdminRuleListResponse result = adminRuleService.getRuleList(2, 5, "duty", "active", "  night  ");

        // then
        assertThat(result.rules()).hasSize(1);
        assertThat(result.selectedCategory()).isEqualTo("DUTY");
        assertThat(result.selectedActive()).isEqualTo("ACTIVE");
        assertThat(result.keyword()).isEqualTo("night");
        assertThat(result.currentPage()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.hasPages()).isTrue();
        assertThat(result.hasPrevious()).isTrue();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.pageLinks()).hasSize(2);
        assertThat(result.pageLinks().get(1).active()).isTrue();
        assertThat(result.categoryOptions()).extracting(AdminRuleFilterOptionResponse::label)
                .containsExactly("전체", "응급", "물품", "근무", "위생", "기타");
        assertThat(result.activeOptions()).extracting(AdminRuleFilterOptionResponse::label)
                .containsExactly("전체", "활성", "비활성");
    }

    @Test
    @DisplayName("filtered rule list uses default values for invalid params")
    void getRuleList_withInvalidParams_usesDefaults() {
        // given
        var page = new PageImpl<HospitalRule>(List.of(), PageRequest.of(0, 10), 0);
        given(hospitalRuleRepository.search(null, null, "", PageRequest.of(0, 10))).willReturn(page);

        // when
        AdminRuleListResponse result = adminRuleService.getRuleList(0, 0, "wrong", "wrong", "   ");

        // then
        assertThat(result.selectedCategory()).isEqualTo("ALL");
        assertThat(result.selectedActive()).isEqualTo("ALL");
        assertThat(result.keyword()).isEmpty();
        assertThat(result.currentPage()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalCount()).isZero();
        assertThat(result.rules()).isEmpty();
        assertThat(result.categoryOptions().get(0).selected()).isTrue();
        assertThat(result.activeOptions().get(0).selected()).isTrue();
    }

    @Test
    @DisplayName("createRule saves rule with correct category")
    void createRule_savesRuleWithCorrectCategory() {
        // given

        // when
        adminRuleService.createRule("근무 지침", "야간 근무 시 주의사항", "DUTY");

        // then
        then(hospitalRuleRepository).should().save(any(HospitalRule.class));
    }

    @Test
    @DisplayName("AdminRuleDto shows active text for active rule")
    void adminRuleDto_activeRule_showsActiveText() {
        // given
        HospitalRule rule = HospitalRule.create("위생 규칙", "손씻기 필수", HospitalRuleCategory.HYGIENE);

        // when
        AdminRuleDto dto = new AdminRuleDto(rule);

        // then
        assertThat(dto.isActive()).isTrue();
        assertThat(dto.getActiveText()).isEqualTo("활성");
    }

    @Test
    @DisplayName("AdminRuleDto converts category text correctly")
    void adminRuleDto_categoryText_convertsCorrectly() {
        // given // when // then
        assertThat(new AdminRuleDto(HospitalRule.create("t", "c", HospitalRuleCategory.EMERGENCY)).getCategoryText()).isEqualTo("응급");
        assertThat(new AdminRuleDto(HospitalRule.create("t", "c", HospitalRuleCategory.SUPPLY)).getCategoryText()).isEqualTo("물품");
        assertThat(new AdminRuleDto(HospitalRule.create("t", "c", HospitalRuleCategory.DUTY)).getCategoryText()).isEqualTo("근무");
        assertThat(new AdminRuleDto(HospitalRule.create("t", "c", HospitalRuleCategory.HYGIENE)).getCategoryText()).isEqualTo("위생");
        assertThat(new AdminRuleDto(HospitalRule.create("t", "c", HospitalRuleCategory.OTHER)).getCategoryText()).isEqualTo("기타");
    }
}
