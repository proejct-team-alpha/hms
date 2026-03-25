package com.smartclinic.hms.admin.rule;

import com.smartclinic.hms.admin.rule.dto.AdminRuleDeleteResponse;
import com.smartclinic.hms.common.AdminControllerTestSecurityConfig;
import com.smartclinic.hms.common.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminRuleApiController.class)
@Import(AdminControllerTestSecurityConfig.class)
class AdminRuleApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminRuleService adminRuleService;

    @Test
    @DisplayName("admin can delete rule via admin api")
    void deleteRule_success() throws Exception {
        // given
        given(adminRuleService.deleteRule(10L))
                .willReturn(AdminRuleDeleteResponse.success(10L));

        // when
        // then
        mockMvc.perform(post("/admin/api/rules/10")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.body.ruleId").value(10))
                .andExpect(jsonPath("$.body.message").value("규칙이 삭제되었습니다."));

        then(adminRuleService).should().deleteRule(10L);
    }

    @Test
    @DisplayName("non-admin cannot delete rule via admin api")
    void deleteRule_forbiddenWhenNotAdmin() throws Exception {
        // given

        // when
        // then
        mockMvc.perform(post("/admin/api/rules/10")
                        .with(user("staff").roles("STAFF")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminRuleService);
    }

    @Test
    @DisplayName("returns 404 when rule does not exist")
    void deleteRule_notFound() throws Exception {
        // given
        given(adminRuleService.deleteRule(999L))
                .willThrow(CustomException.notFound("규칙을 찾을 수 없습니다."));

        // when
        // then
        mockMvc.perform(post("/admin/api/rules/999")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.msg", containsString("RESOURCE_NOT_FOUND")))
                .andExpect(jsonPath("$.msg", containsString("규칙을 찾을 수 없습니다.")));
    }
}
