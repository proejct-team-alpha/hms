package com.smartclinic.hms.admin.dashboard;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.smartclinic.hms.admin.dashboard.dto.AdminDashboardChartResponse;
import com.smartclinic.hms.admin.dashboard.dto.AdminDashboardStatsResponse;

@WebMvcTest(AdminDashboardApiController.class)
@Import(AdminDashboardApiControllerTest.TestSecurityConfig.class)
class AdminDashboardApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminDashboardStatsService adminDashboardStatsService;

    @BeforeEach
    void setUp() {
        given(adminDashboardStatsService.getDashboardStats())
                .willReturn(new AdminDashboardStatsResponse(7L, 70L, 12L, 4L));
        given(adminDashboardStatsService.getDashboardChart())
                .willReturn(new AdminDashboardChartResponse(
                        java.util.List.of(
                                new AdminDashboardChartResponse.CategoryCount("Medical Supplies", 5L),
                                new AdminDashboardChartResponse.CategoryCount("Medical Equipment", 2L)),
                        java.util.List.of(
                                new AdminDashboardChartResponse.DailyPatientCount(
                                        java.time.LocalDate.of(2026, 3, 2), 0L),
                                new AdminDashboardChartResponse.DailyPatientCount(
                                        java.time.LocalDate.of(2026, 3, 3), 3L))));
    }

    @Test
    @DisplayName("ROLE_ADMIN can fetch dashboard stats as JSON")
    void dashboardStats_withAdminRole_returnsJson() throws Exception {
        // given

        // when

        // then
        mockMvc.perform(get("/admin/dashboard/stats").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").isString())
                .andExpect(jsonPath("$.body.todayReservations").value(7))
                .andExpect(jsonPath("$.body.totalReservations").value(70))
                .andExpect(jsonPath("$.body.totalStaff").value(12))
                .andExpect(jsonPath("$.body.lowStockItems").value(4));
    }

    @Test
    @DisplayName("ROLE_STAFF is forbidden from dashboard stats JSON")
    void dashboardStats_withNonAdminRole_isForbidden() throws Exception {
        // given

        // when

        // then
        mockMvc.perform(get("/admin/dashboard/stats").with(user("staff").roles("STAFF")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_ADMIN can fetch dashboard chart as JSON")
    void dashboardChart_withAdminRole_returnsJson() throws Exception {
        // given

        // when

        // then
        mockMvc.perform(get("/admin/dashboard/chart").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").isString())
                .andExpect(jsonPath("$.body.categoryCounts[0].categoryName").value("Medical Supplies"))
                .andExpect(jsonPath("$.body.categoryCounts[0].totalCount").value(5))
                .andExpect(jsonPath("$.body.dailyPatients[0].date").value("2026-03-02"))
                .andExpect(jsonPath("$.body.dailyPatients[0].patientCount").value(0))
                .andExpect(jsonPath("$.body.dailyPatients[1].date").value("2026-03-03"))
                .andExpect(jsonPath("$.body.dailyPatients[1].patientCount").value(3));
    }

    @Test
    @DisplayName("ROLE_STAFF is forbidden from dashboard chart JSON")
    void dashboardChart_withNonAdminRole_isForbidden() throws Exception {
        // given

        // when

        // then
        mockMvc.perform(get("/admin/dashboard/chart").with(user("staff").roles("STAFF")))
                .andExpect(status().isForbidden());
    }

    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/login").permitAll()
                            .requestMatchers("/admin/**").hasRole("ADMIN")
                            .anyRequest().authenticated())
                    .csrf(csrf -> csrf.disable())
                    .build();
        }

        @Bean
        UserDetailsService userDetailsService() {
            return new InMemoryUserDetailsManager(
                    User.withUsername("admin").password("{noop}password").roles("ADMIN").build(),
                    User.withUsername("staff").password("{noop}password").roles("STAFF").build());
        }
    }
}
