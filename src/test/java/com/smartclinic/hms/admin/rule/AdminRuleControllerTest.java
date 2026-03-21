package com.smartclinic.hms.admin.rule;

import com.smartclinic.hms.admin.rule.dto.AdminRuleFilterOptionResponse;
import com.smartclinic.hms.admin.rule.dto.AdminRuleItemResponse;
import com.smartclinic.hms.admin.rule.dto.AdminRuleListResponse;
import com.smartclinic.hms.admin.rule.dto.AdminRulePageLinkResponse;
import com.smartclinic.hms.admin.rule.dto.CreateAdminRuleRequest;
import com.smartclinic.hms.common.AdminControllerTestSecurityConfig;
import com.smartclinic.hms.common.util.SsrValidationViewSupport;
import com.smartclinic.hms.domain.HospitalRule;
import com.smartclinic.hms.domain.HospitalRuleCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(value = AdminRuleController.class, properties = {
        "spring.mustache.servlet.expose-request-attributes=true",
        "spring.mustache.servlet.allow-request-override=true"
})
@Import(AdminControllerTestSecurityConfig.class)
class AdminRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminRuleService adminRuleService;

    @Test
    @DisplayName("admin can access rule list with default filters")
    void ruleList_withAdminRole_returnsOk() throws Exception {
        // given
        AdminRuleListResponse response = createEmptyResponse();
        given(adminRuleService.getRuleList(1, 10, "ALL", "ALL", null)).willReturn(response);

        // when
        // then
        mockMvc.perform(get("/admin/rule/list")
                        .with(user("admin01").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/rule-list"))
                .andExpect(request().attribute("model", response))
                .andExpect(content().string(containsString("\uC81C\uBAA9 \uAC80\uC0C9")))
                .andExpect(content().string(containsString("\uCE74\uD14C\uACE0\uB9AC")))
                .andExpect(content().string(containsString("\uD65C\uC131 \uC5EC\uBD80")))
                .andExpect(content().string(containsString("href=\"/admin/rule/new\"")))
                .andExpect(content().string(containsString("\uC870\uD68C\uB41C \uADDC\uCE59\uC774 \uC5C6\uC2B5\uB2C8\uB2E4.")));

        then(adminRuleService).should().getRuleList(1, 10, "ALL", "ALL", null);
    }

    @Test
    @DisplayName("rule list passes filter params to service and renders selected filters")
    void ruleList_passesFilterParamsToService() throws Exception {
        // given
        AdminRuleListResponse response = createFilteredResponse();
        given(adminRuleService.getRuleList(2, 5, "DUTY", "ACTIVE", "night")).willReturn(response);

        // when
        // then
        mockMvc.perform(get("/admin/rule/list")
                        .param("page", "2")
                        .param("size", "5")
                        .param("category", "DUTY")
                        .param("active", "ACTIVE")
                        .param("keyword", "night")
                        .with(user("admin01").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/rule-list"))
                .andExpect(request().attribute("model", response))
                .andExpect(content().string(containsString("night-duty-rule")))
                .andExpect(content().string(containsString("value=\"DUTY\" selected")))
                .andExpect(content().string(containsString("value=\"ACTIVE\" selected")))
                .andExpect(content().string(containsString("value=\"night\"")))
                .andExpect(content().string(containsString(">1</a>")))
                .andExpect(content().string(containsString(">2</a>")));

        then(adminRuleService).should().getRuleList(2, 5, "DUTY", "ACTIVE", "night");
    }

    @Test
    @DisplayName("unauthenticated user is redirected to login from rule list")
    void ruleList_withoutAuthentication_redirectsToLogin() throws Exception {
        // given

        // when
        // then
        mockMvc.perform(get("/admin/rule/list"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("doctor cannot access rule list")
    void ruleList_withDoctorRole_isForbidden() throws Exception {
        // given

        // when
        // then
        mockMvc.perform(get("/admin/rule/list")
                        .with(user("doctor01").roles("DOCTOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("admin can open new rule form")
    void newForm_withAdminRole_returnsOk() throws Exception {
        // given
        CreateAdminRuleRequest defaultForm = CreateAdminRuleRequest.defaultForm();

        // when
        // then
        mockMvc.perform(get("/admin/rule/new")
                        .with(user("admin01").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/rule-new"))
                .andExpect(request().attribute("model", defaultForm))
                .andExpect(request().attribute("activeChecked", true))
                .andExpect(content().string(containsString("action=\"/admin/rule/new\"")))
                .andExpect(content().string(containsString("name=\"active\"")))
                .andExpect(content().string(containsString("checked")));
    }

    @Test
    @DisplayName("legacy form path redirects to new form")
    void form_withLegacyPath_redirectsToNewForm() throws Exception {
        // given

        // when
        // then
        mockMvc.perform(get("/admin/rule/form")
                        .with(user("admin01").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/rule/new"));
    }

    @Test
    @DisplayName("admin can create rule through new path and is redirected to list")
    void createRule_withNewPath_redirectsToList() throws Exception {
        // given
        CreateAdminRuleRequest request = new CreateAdminRuleRequest(
                "\uC751\uAE09 \uC9C0\uCE68",
                "\uC751\uAE09\uC2E4 \uC6B0\uC120 \uB300\uC751 \uC548\uB0B4",
                HospitalRuleCategory.EMERGENCY,
                Boolean.TRUE
        );
        given(adminRuleService.createRule(any(CreateAdminRuleRequest.class))).willReturn("\uADDC\uCE59\uC774 \uB4F1\uB85D\uB418\uC5C8\uC2B5\uB2C8\uB2E4.");

        // when
        // then
        mockMvc.perform(post("/admin/rule/new")
                        .param("title", "\uC751\uAE09 \uC9C0\uCE68")
                        .param("content", "\uC751\uAE09\uC2E4 \uC6B0\uC120 \uB300\uC751 \uC548\uB0B4")
                        .param("category", "EMERGENCY")
                        .param("active", "true")
                        .with(user("admin01").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/admin/rule/list"))
                .andExpect(flash().attribute("successMessage", "\uADDC\uCE59\uC774 \uB4F1\uB85D\uB418\uC5C8\uC2B5\uB2C8\uB2E4."));

        then(adminRuleService).should().createRule(request);
    }

    @Test
    @DisplayName("new rule validation failure rerenders form and does not call service")
    void createRule_withValidationFailure_rerendersForm() throws Exception {
        // given
        CreateAdminRuleRequest invalidRequest = new CreateAdminRuleRequest(" ", "", null, null);

        // when
        // then
        mockMvc.perform(post("/admin/rule/new")
                        .param("title", " ")
                        .param("content", "")
                        .with(user("admin01").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/rule-new"))
                .andExpect(request().attribute("model", invalidRequest))
                .andExpect(request().attribute("errorMessage", SsrValidationViewSupport.INPUT_CHECK_MESSAGE))
                .andExpect(request().attribute("titleError", "\uC81C\uBAA9\uC740 \uD544\uC218\uC785\uB2C8\uB2E4."))
                .andExpect(request().attribute("contentError", "\uB0B4\uC6A9\uC740 \uD544\uC218\uC785\uB2C8\uB2E4."))
                .andExpect(request().attribute("categoryError", "\uCE74\uD14C\uACE0\uB9AC\uB97C \uC120\uD0DD\uD574 \uC8FC\uC138\uC694."))
                .andExpect(request().attribute("activeChecked", false));

        then(adminRuleService).should(never()).createRule(any(CreateAdminRuleRequest.class));
    }

    @Test
    @DisplayName("new rule invalid category rerenders form with friendly message")
    void createRule_withInvalidCategory_rerendersFormWithFriendlyMessage() throws Exception {
        // given
        CreateAdminRuleRequest invalidRequest = new CreateAdminRuleRequest(
                "\uC751\uAE09 \uC9C0\uCE68",
                "\uC751\uAE09\uC2E4 \uB300\uC751 \uC548\uB0B4",
                null,
                Boolean.TRUE
        );

        // when
        // then
        mockMvc.perform(post("/admin/rule/new")
                        .param("title", "\uC751\uAE09 \uC9C0\uCE68")
                        .param("content", "\uC751\uAE09\uC2E4 \uB300\uC751 \uC548\uB0B4")
                        .param("category", "WRONG")
                        .param("active", "true")
                        .with(user("admin01").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/rule-new"))
                .andExpect(request().attribute("model", invalidRequest))
                .andExpect(request().attribute("errorMessage", SsrValidationViewSupport.INPUT_CHECK_MESSAGE))
                .andExpect(request().attribute("categoryError", "\uC62C\uBC14\uB978 \uCE74\uD14C\uACE0\uB9AC\uB97C \uC120\uD0DD\uD574 \uC8FC\uC138\uC694."))
                .andExpect(request().attribute("activeChecked", true));

        then(adminRuleService).should(never()).createRule(any(CreateAdminRuleRequest.class));
    }

    @Test
    @DisplayName("legacy form post validation failure rerenders new form and does not call service")
    void createRule_withLegacyPathValidationFailure_rerendersNewForm() throws Exception {
        // given
        CreateAdminRuleRequest invalidRequest = new CreateAdminRuleRequest(" ", "", null, null);

        // when
        // then
        mockMvc.perform(post("/admin/rule/form")
                        .param("title", " ")
                        .param("content", "")
                        .with(user("admin01").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/rule-new"))
                .andExpect(request().attribute("model", invalidRequest))
                .andExpect(request().attribute("errorMessage", SsrValidationViewSupport.INPUT_CHECK_MESSAGE))
                .andExpect(request().attribute("titleError", "\uC81C\uBAA9\uC740 \uD544\uC218\uC785\uB2C8\uB2E4."))
                .andExpect(request().attribute("contentError", "\uB0B4\uC6A9\uC740 \uD544\uC218\uC785\uB2C8\uB2E4."))
                .andExpect(request().attribute("categoryError", "\uCE74\uD14C\uACE0\uB9AC\uB97C \uC120\uD0DD\uD574 \uC8FC\uC138\uC694."))
                .andExpect(request().attribute("activeChecked", false));

        then(adminRuleService).should(never()).createRule(any(CreateAdminRuleRequest.class));
    }

    @Test
    @DisplayName("legacy form post invalid category rerenders new form with friendly message")
    void createRule_withLegacyPathInvalidCategory_rerendersNewFormWithFriendlyMessage() throws Exception {
        // given
        CreateAdminRuleRequest invalidRequest = new CreateAdminRuleRequest(
                "\uADFC\uBB34 \uC778\uC218\uC778\uACC4",
                "\uAD50\uB300 \uC804 \uC778\uC218\uC778\uACC4 \uB0B4\uC6A9",
                null,
                Boolean.TRUE
        );

        // when
        // then
        mockMvc.perform(post("/admin/rule/form")
                        .param("title", "\uADFC\uBB34 \uC778\uC218\uC778\uACC4")
                        .param("content", "\uAD50\uB300 \uC804 \uC778\uC218\uC778\uACC4 \uB0B4\uC6A9")
                        .param("category", "WRONG")
                        .param("active", "true")
                        .with(user("admin01").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/rule-new"))
                .andExpect(request().attribute("model", invalidRequest))
                .andExpect(request().attribute("errorMessage", SsrValidationViewSupport.INPUT_CHECK_MESSAGE))
                .andExpect(request().attribute("categoryError", "\uC62C\uBC14\uB978 \uCE74\uD14C\uACE0\uB9AC\uB97C \uC120\uD0DD\uD574 \uC8FC\uC138\uC694."))
                .andExpect(request().attribute("activeChecked", true));

        then(adminRuleService).should(never()).createRule(any(CreateAdminRuleRequest.class));
    }

    @Test
    @DisplayName("legacy form post reuses same create flow")
    void createRule_withLegacyPath_redirectsToList() throws Exception {
        // given
        CreateAdminRuleRequest request = new CreateAdminRuleRequest(
                "\uADFC\uBB34 \uC778\uC218\uC778\uACC4",
                "\uAD50\uB300 \uC804 \uC778\uC218\uC778\uACC4 \uB0B4\uC6A9\uC744 \uAE30\uB85D\uD55C\uB2E4",
                HospitalRuleCategory.DUTY,
                Boolean.TRUE
        );
        given(adminRuleService.createRule(any(CreateAdminRuleRequest.class))).willReturn("\uADDC\uCE59\uC774 \uB4F1\uB85D\uB418\uC5C8\uC2B5\uB2C8\uB2E4.");

        // when
        // then
        mockMvc.perform(post("/admin/rule/form")
                        .param("title", "\uADFC\uBB34 \uC778\uC218\uC778\uACC4")
                        .param("content", "\uAD50\uB300 \uC804 \uC778\uC218\uC778\uACC4 \uB0B4\uC6A9\uC744 \uAE30\uB85D\uD55C\uB2E4")
                        .param("category", "DUTY")
                        .param("active", "true")
                        .with(user("admin01").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/admin/rule/list"))
                .andExpect(flash().attribute("successMessage", "\uADDC\uCE59\uC774 \uB4F1\uB85D\uB418\uC5C8\uC2B5\uB2C8\uB2E4."));

        then(adminRuleService).should().createRule(request);
    }

    private AdminRuleListResponse createEmptyResponse() {
        return new AdminRuleListResponse(
                List.of(),
                List.of(),
                List.of(new AdminRuleFilterOptionResponse("ALL", "\uC804\uCCB4", true)),
                List.of(new AdminRuleFilterOptionResponse("ALL", "\uC804\uCCB4", true)),
                "ALL",
                "ALL",
                "",
                0,
                1,
                10,
                0,
                false,
                false,
                false,
                "",
                ""
        );
    }

    private AdminRuleListResponse createFilteredResponse() {
        HospitalRule rule = HospitalRule.create("night-duty-rule", "night shift handoff guideline", HospitalRuleCategory.DUTY, true);
        AdminRuleItemResponse dto = new AdminRuleItemResponse(rule);

        return new AdminRuleListResponse(
                List.of(dto),
                List.of(
                        new AdminRulePageLinkResponse(1, "/admin/rule/list?page=1&size=5&category=DUTY&active=ACTIVE&keyword=night", false),
                        new AdminRulePageLinkResponse(2, "/admin/rule/list?page=2&size=5&category=DUTY&active=ACTIVE&keyword=night", true)
                ),
                List.of(
                        new AdminRuleFilterOptionResponse("ALL", "\uC804\uCCB4", false),
                        new AdminRuleFilterOptionResponse("DUTY", "\uADFC\uBB34", true)
                ),
                List.of(
                        new AdminRuleFilterOptionResponse("ALL", "\uC804\uCCB4", false),
                        new AdminRuleFilterOptionResponse("ACTIVE", "\uD65C\uC131", true)
                ),
                "DUTY",
                "ACTIVE",
                "night",
                6,
                2,
                5,
                2,
                true,
                true,
                false,
                "/admin/rule/list?page=1&size=5&category=DUTY&active=ACTIVE&keyword=night",
                ""
        );
    }
}
