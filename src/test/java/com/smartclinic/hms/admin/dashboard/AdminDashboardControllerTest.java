package com.smartclinic.hms.admin.dashboard;

import com.smartclinic.hms.admin.dashboard.dto.AdminDashboardStatsResponse;
import com.smartclinic.hms.common.AdminControllerTestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(
        value = AdminDashboardController.class,
        properties = {
                "spring.mustache.servlet.expose-request-attributes=true",
                "spring.mustache.servlet.allow-request-override=true"
        }
)
@Import(AdminControllerTestSecurityConfig.class)
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminDashboardStatsService adminDashboardStatsService;

    @BeforeEach
    void setUp() {
        given(adminDashboardStatsService.getDashboardStats())
                .willReturn(new AdminDashboardStatsResponse(7L, 70L, 12L, 4L));
    }

    @Test
    @DisplayName("ROLE_ADMIN can render admin dashboard")
    void dashboard_withAdminRole_rendersDashboardView() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(request().attribute("model", new AdminDashboardStatsResponse(7L, 70L, 12L, 4L)))
                .andExpect(request().attribute("pageTitle", "\uAD00\uB9AC\uC790 \uB300\uC2DC\uBCF4\uB4DC"))
                .andExpect(content().string(containsString("\uC77C\uC77C \uD658\uC790\uC218 \uCD94\uC774")))
                .andExpect(content().string(containsString("\uCD5C\uADFC 7\uC77C \uC785\uACE0/\uCD9C\uACE0 \uCD94\uC774")))
                .andExpect(content().string(containsString("daily-patient-canvas")))
                .andExpect(content().string(containsString("item-flow-chart")))
                .andExpect(content().string(containsString("/js/pages/admin-dashboard.js")));
    }

    @Test
    @DisplayName("Unauthenticated user is redirected to login page")
    void dashboard_withoutAuthentication_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("ROLE_STAFF is forbidden from admin dashboard")
    void dashboard_withNonAdminRole_isForbidden() throws Exception {
        mockMvc.perform(get("/admin/dashboard").with(user("staff").roles("STAFF")))
                .andExpect(status().isForbidden());
    }
}