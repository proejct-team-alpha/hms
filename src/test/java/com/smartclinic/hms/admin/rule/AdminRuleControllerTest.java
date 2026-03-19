package com.smartclinic.hms.admin.rule;

import com.smartclinic.hms.common.AdminControllerTestSecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminRuleController.class)
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

        // when // then
        mockMvc.perform(get("/admin/rule/list")
                        .with(user("admin01").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/rule-list"))
                .andExpect(model().attribute("model", response));

        then(adminRuleService).should().getRuleList(1, 10, "ALL", "ALL", null);
    }

    @Test
    @DisplayName("rule list passes filter params to service")
    void ruleList_passesFilterParamsToService() throws Exception {
        // given
        AdminRuleListResponse response = createEmptyResponse();
        given(adminRuleService.getRuleList(2, 5, "DUTY", "ACTIVE", "night")).willReturn(response);

        // when // then
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
                .andExpect(model().attribute("model", response));

        then(adminRuleService).should().getRuleList(2, 5, "DUTY", "ACTIVE", "night");
    }

    @Test
    @DisplayName("unauthenticated user is redirected to login from rule list")
    void ruleList_withoutAuthentication_redirectsToLogin() throws Exception {
        // given

        // when // then
        mockMvc.perform(get("/admin/rule/list"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("doctor cannot access rule list")
    void ruleList_withDoctorRole_isForbidden() throws Exception {
        // given

        // when // then
        mockMvc.perform(get("/admin/rule/list")
                        .with(user("doctor01").roles("DOCTOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("admin can create rule and is redirected to list")
    void createRule_withAdminRole_redirectsToList() throws Exception {
        // given

        // when // then
        mockMvc.perform(post("/admin/rule/form")
                        .param("title", "응급 지침")
                        .param("content", "응급실 우선 대응 안내")
                        .param("category", "EMERGENCY")
                        .with(user("admin01").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrlPattern("/admin/rule/list*"));

        then(adminRuleService).should().createRule("응급 지침", "응급실 우선 대응 안내", "EMERGENCY");
    }

    private AdminRuleListResponse createEmptyResponse() {
        return new AdminRuleListResponse(
                List.of(),
                List.of(),
                List.of(new AdminRuleFilterOptionResponse("ALL", "전체", true)),
                List.of(new AdminRuleFilterOptionResponse("ALL", "전체", true)),
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
}
