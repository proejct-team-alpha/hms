package com.smartclinic.hms.admin.rule;

import com.smartclinic.hms.domain.HospitalRule;
import com.smartclinic.hms.domain.HospitalRuleCategory;
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
class AdminRuleServiceTest {

    @Mock
    private HospitalRuleRepository hospitalRuleRepository;

    @InjectMocks
    private AdminRuleService adminRuleService;

    // ── getRuleList ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getRuleList — 전체 규칙 목록을 DTO로 변환하여 반환")
    void getRuleList_returnsMappedDtos() {
        HospitalRule rule1 = HospitalRule.create("응급 절차", "응급 시 이렇게 하세요", HospitalRuleCategory.EMERGENCY);
        HospitalRule rule2 = HospitalRule.create("물품 관리", "물품 정리 방법", HospitalRuleCategory.SUPPLY);
        given(hospitalRuleRepository.findAllByOrderByCreatedAtDesc()).willReturn(List.of(rule1, rule2));

        List<AdminRuleDto> result = adminRuleService.getRuleList();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("응급 절차");
        assertThat(result.get(0).getCategoryText()).isEqualTo("응급");
        assertThat(result.get(1).getTitle()).isEqualTo("물품 관리");
        assertThat(result.get(1).getCategoryText()).isEqualTo("물품");
    }

    @Test
    @DisplayName("getRuleList — 규칙이 없으면 빈 목록 반환")
    void getRuleList_withNoRules_returnsEmpty() {
        given(hospitalRuleRepository.findAllByOrderByCreatedAtDesc()).willReturn(List.of());

        List<AdminRuleDto> result = adminRuleService.getRuleList();

        assertThat(result).isEmpty();
    }

    // ── createRule ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("createRule — 올바른 카테고리로 규칙 저장")
    void createRule_savesRuleWithCorrectCategory() {
        adminRuleService.createRule("근무 지침", "야간 근무 시 주의사항", "DUTY");

        then(hospitalRuleRepository).should().save(any(HospitalRule.class));
    }

    // ── AdminRuleDto ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("AdminRuleDto — 활성 규칙은 '활성' 텍스트를 가짐")
    void adminRuleDto_activeRule_showsActiveText() {
        HospitalRule rule = HospitalRule.create("위생 규칙", "손 씻기 필수", HospitalRuleCategory.HYGIENE);

        AdminRuleDto dto = new AdminRuleDto(rule);

        assertThat(dto.isActive()).isTrue();
        assertThat(dto.getActiveText()).isEqualTo("활성");
    }

    @Test
    @DisplayName("AdminRuleDto — 각 카테고리를 한국어로 변환")
    void adminRuleDto_categoryText_convertsCorrectly() {
        assertThat(new AdminRuleDto(HospitalRule.create("t", "c", HospitalRuleCategory.EMERGENCY)).getCategoryText()).isEqualTo("응급");
        assertThat(new AdminRuleDto(HospitalRule.create("t", "c", HospitalRuleCategory.SUPPLY)).getCategoryText()).isEqualTo("물품");
        assertThat(new AdminRuleDto(HospitalRule.create("t", "c", HospitalRuleCategory.DUTY)).getCategoryText()).isEqualTo("근무");
        assertThat(new AdminRuleDto(HospitalRule.create("t", "c", HospitalRuleCategory.HYGIENE)).getCategoryText()).isEqualTo("위생");
        assertThat(new AdminRuleDto(HospitalRule.create("t", "c", HospitalRuleCategory.OTHER)).getCategoryText()).isEqualTo("기타");
    }
}
