package com.smartclinic.hms.admin.rule;

import com.smartclinic.hms.admin.rule.dto.AdminRuleFilterOptionResponse;
import com.smartclinic.hms.admin.rule.dto.AdminRuleItemResponse;
import com.smartclinic.hms.admin.rule.dto.AdminRuleListResponse;
import com.smartclinic.hms.admin.rule.dto.AdminRulePageLinkResponse;
import com.smartclinic.hms.admin.rule.dto.CreateAdminRuleRequest;
import com.smartclinic.hms.domain.HospitalRule;
import com.smartclinic.hms.domain.HospitalRuleCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
        HospitalRule rule1 = HospitalRule.create("emergency-rule", "emergency first response", HospitalRuleCategory.EMERGENCY, true);
        HospitalRule rule2 = HospitalRule.create("supply-rule", "supply arrangement guide", HospitalRuleCategory.SUPPLY, true);
        given(hospitalRuleRepository.findAllByOrderByCreatedAtDesc()).willReturn(List.of(rule1, rule2));

        // when
        List<AdminRuleItemResponse> result = adminRuleService.getRuleList();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("emergency-rule");
        assertThat(result.get(0).getCategoryText()).isEqualTo("\uC751\uAE09");
        assertThat(result.get(1).getTitle()).isEqualTo("supply-rule");
        assertThat(result.get(1).getCategoryText()).isEqualTo("\uBB3C\uD488");
    }

    @Test
    @DisplayName("getRuleList returns empty list when no rule exists")
    void getRuleList_withNoRules_returnsEmpty() {
        // given
        given(hospitalRuleRepository.findAllByOrderByCreatedAtDesc()).willReturn(List.of());

        // when
        List<AdminRuleItemResponse> result = adminRuleService.getRuleList();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("filtered rule list normalizes params and returns paging metadata")
    void getRuleList_withFilters_returnsPagedResponse() {
        // given
        HospitalRule rule = HospitalRule.create("night-duty-rule", "night shift handoff guideline", HospitalRuleCategory.DUTY, true);
        var page = new PageImpl<>(List.of(rule), PageRequest.of(1, 5), 12);
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
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.hasPages()).isTrue();
        assertThat(result.hasPrevious()).isTrue();
        assertThat(result.hasNext()).isTrue();
        assertThat(result.previousUrl()).isEqualTo("/admin/rule/list?page=1&size=5&category=DUTY&active=ACTIVE&keyword=night");
        assertThat(result.nextUrl()).isEqualTo("/admin/rule/list?page=3&size=5&category=DUTY&active=ACTIVE&keyword=night");
        assertThat(result.pageLinks()).hasSize(3);
        assertThat(result.pageLinks().get(0).url()).isEqualTo("/admin/rule/list?page=1&size=5&category=DUTY&active=ACTIVE&keyword=night");
        assertThat(result.pageLinks().get(1).active()).isTrue();
        assertThat(result.categoryOptions()).extracting(AdminRuleFilterOptionResponse::label)
                .containsExactly("\uC804\uCCB4", "\uC751\uAE09", "\uBB3C\uD488", "\uADFC\uBB34", "\uC704\uC0DD", "\uAE30\uD0C0");
        assertThat(result.activeOptions()).extracting(AdminRuleFilterOptionResponse::label)
                .containsExactly("\uC804\uCCB4", "\uD65C\uC131", "\uBE44\uD65C\uC131");
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
        assertThat(result.hasPages()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.pageLinks()).isEmpty();
        assertThat(result.categoryOptions().get(0).selected()).isTrue();
        assertThat(result.activeOptions().get(0).selected()).isTrue();
    }

    @Test
    @DisplayName("page links show first five pages near the beginning")
    void getRuleList_nearBeginning_showsFirstFivePageLinks() {
        // given
        var page = new PageImpl<HospitalRule>(List.of(), PageRequest.of(0, 10), 120);
        given(hospitalRuleRepository.search(null, null, "", PageRequest.of(0, 10))).willReturn(page);

        // when
        AdminRuleListResponse result = adminRuleService.getRuleList(1, 10, "ALL", "ALL", "");

        // then
        assertThat(result.pageLinks()).extracting(AdminRulePageLinkResponse::page)
                .containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    @DisplayName("page links slide around the current page")
    void getRuleList_nearMiddle_slidesPageLinksAroundCurrentPage() {
        // given
        var page = new PageImpl<HospitalRule>(List.of(), PageRequest.of(4, 10), 120);
        given(hospitalRuleRepository.search(null, null, "", PageRequest.of(4, 10))).willReturn(page);

        // when
        AdminRuleListResponse result = adminRuleService.getRuleList(5, 10, "ALL", "ALL", "");

        // then
        assertThat(result.pageLinks()).extracting(AdminRulePageLinkResponse::page)
                .containsExactly(3, 4, 5, 6, 7);
        assertThat(result.pageLinks().get(2).active()).isTrue();
    }

    @Test
    @DisplayName("page links show the last five pages near the end")
    void getRuleList_nearEnd_showsLastFivePageLinks() {
        // given
        var page = new PageImpl<HospitalRule>(List.of(), PageRequest.of(11, 10), 120);
        given(hospitalRuleRepository.search(null, null, "", PageRequest.of(11, 10))).willReturn(page);

        // when
        AdminRuleListResponse result = adminRuleService.getRuleList(12, 10, "ALL", "ALL", "");

        // then
        assertThat(result.pageLinks()).extracting(AdminRulePageLinkResponse::page)
                .containsExactly(8, 9, 10, 11, 12);
        assertThat(result.pageLinks().get(4).active()).isTrue();
    }

    @Test
    @DisplayName("createRule trims fields and saves active rule")
    void createRule_trimsFieldsAndSavesActiveRule() {
        // given
        CreateAdminRuleRequest request = new CreateAdminRuleRequest(
                "  duty-rule  ",
                "  night shift warning  ",
                HospitalRuleCategory.DUTY,
                Boolean.TRUE
        );
        ArgumentCaptor<HospitalRule> captor = ArgumentCaptor.forClass(HospitalRule.class);

        // when
        String result = adminRuleService.createRule(request);

        // then
        then(hospitalRuleRepository).should().save(captor.capture());
        HospitalRule savedRule = captor.getValue();
        assertThat(result).isEqualTo("\uADDC\uCE59\uC774 \uB4F1\uB85D\uB418\uC5C8\uC2B5\uB2C8\uB2E4.");
        assertThat(savedRule.getTitle()).isEqualTo("duty-rule");
        assertThat(savedRule.getContent()).isEqualTo("night shift warning");
        assertThat(savedRule.getCategory()).isEqualTo(HospitalRuleCategory.DUTY);
        assertThat(savedRule.isActive()).isTrue();
    }

    @Test
    @DisplayName("createRule saves inactive rule when active is false")
    void createRule_withUncheckedActive_savesInactiveRule() {
        // given
        CreateAdminRuleRequest request = new CreateAdminRuleRequest(
                "supply-rule",
                "store cold chain items separately",
                HospitalRuleCategory.SUPPLY,
                Boolean.FALSE
        );
        ArgumentCaptor<HospitalRule> captor = ArgumentCaptor.forClass(HospitalRule.class);

        // when
        adminRuleService.createRule(request);

        // then
        then(hospitalRuleRepository).should().save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
    }

    @Test
    @DisplayName("createRule saves inactive rule when active is null")
    void createRule_withMissingActive_savesInactiveRule() {
        // given
        CreateAdminRuleRequest request = new CreateAdminRuleRequest(
                "hygiene-rule",
                "wash hands before entering the ward",
                HospitalRuleCategory.HYGIENE,
                null
        );
        ArgumentCaptor<HospitalRule> captor = ArgumentCaptor.forClass(HospitalRule.class);

        // when
        adminRuleService.createRule(request);

        // then
        then(hospitalRuleRepository).should().save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
    }

    @Test
    @DisplayName("AdminRuleItemResponse shows active text for active rule")
    void adminRuleItemResponse_activeRule_showsActiveText() {
        // given
        HospitalRule rule = HospitalRule.create("hygiene-rule", "wash hands", HospitalRuleCategory.HYGIENE, true);

        // when
        AdminRuleItemResponse dto = new AdminRuleItemResponse(rule);

        // then
        assertThat(dto.isActive()).isTrue();
        assertThat(dto.getActiveText()).isEqualTo("\uD65C\uC131");
    }

    @Test
    @DisplayName("AdminRuleItemResponse converts category text correctly")
    void adminRuleItemResponse_categoryText_convertsCorrectly() {
        // given
        // when
        // then
        assertThat(new AdminRuleItemResponse(HospitalRule.create("t", "c", HospitalRuleCategory.EMERGENCY, true)).getCategoryText()).isEqualTo("\uC751\uAE09");
        assertThat(new AdminRuleItemResponse(HospitalRule.create("t", "c", HospitalRuleCategory.SUPPLY, true)).getCategoryText()).isEqualTo("\uBB3C\uD488");
        assertThat(new AdminRuleItemResponse(HospitalRule.create("t", "c", HospitalRuleCategory.DUTY, true)).getCategoryText()).isEqualTo("\uADFC\uBB34");
        assertThat(new AdminRuleItemResponse(HospitalRule.create("t", "c", HospitalRuleCategory.HYGIENE, true)).getCategoryText()).isEqualTo("\uC704\uC0DD");
        assertThat(new AdminRuleItemResponse(HospitalRule.create("t", "c", HospitalRuleCategory.OTHER, true)).getCategoryText()).isEqualTo("\uAE30\uD0C0");
    }
}
