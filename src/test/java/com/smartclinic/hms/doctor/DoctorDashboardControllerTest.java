package com.smartclinic.hms.doctor;

import com.smartclinic.hms.doctor.treatment.DoctorTreatmentService;
import com.smartclinic.hms.doctor.treatment.dto.DoctorDashboardDto;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DoctorDashboardController.class)
@Import(DoctorDashboardControllerTest.TestSecurityConfig.class)
class DoctorDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DoctorTreatmentService treatmentService;

    // ── GET /doctor/dashboard ────────────────────────────────────────────────

    @Test
    @DisplayName("ROLE_DOCTOR — 대시보드 접근 시 200 반환")
    void dashboard_withDoctorRole_returnsOk() throws Exception {
        given(treatmentService.getDashboardData(anyString()))
                .willReturn(new DoctorDashboardDto(5, 2, 3, List.of()));

        mockMvc.perform(get("/doctor/dashboard")
                        .with(user("doctor01").roles("DOCTOR"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("doctor/dashboard"));
    }

    @Test
    @DisplayName("미인증 — 대시보드 접근 시 로그인 페이지로 리다이렉트")
    void dashboard_withoutAuthentication_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/doctor/dashboard"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("ROLE_STAFF — 대시보드 접근 시 403 반환")
    void dashboard_withStaffRole_isForbidden() throws Exception {
        mockMvc.perform(get("/doctor/dashboard")
                        .with(user("staff01").roles("STAFF")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_ADMIN — 대시보드 접근 시 200 반환")
    void dashboard_withAdminRole_returnsOk() throws Exception {
        given(treatmentService.getDashboardData(anyString()))
                .willReturn(new DoctorDashboardDto(0, 0, 0, List.of()));

        mockMvc.perform(get("/doctor/dashboard")
                        .with(user("admin01").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    // ── TestSecurityConfig ───────────────────────────────────────────────────

    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/login").permitAll()
                            .requestMatchers("/doctor/**").hasAnyRole("DOCTOR", "ADMIN")
                            .anyRequest().authenticated())
                    .formLogin(form -> form.loginPage("/login").permitAll())
                    .csrf(csrf -> csrf.disable())
                    .build();
        }

        @Bean
        UserDetailsService userDetailsService() {
            return new InMemoryUserDetailsManager(
                    User.withUsername("doctor01").password("{noop}password").roles("DOCTOR").build(),
                    User.withUsername("admin01").password("{noop}password").roles("ADMIN").build(),
                    User.withUsername("staff01").password("{noop}password").roles("STAFF").build());
        }
    }
}
