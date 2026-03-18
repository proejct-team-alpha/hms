package com.smartclinic.hms.doctor.treatment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

@WebMvcTest(DoctorTreatmentController.class)
@Import(DoctorTreatmentControllerTest.TestSecurityConfig.class)
class DoctorTreatmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DoctorTreatmentService treatmentService;

    @Test
    @DisplayName("ROLE_DOCTOR — 폴링 엔드포인트 호출 시 200과 JSON 목록을 반환한다")
    void poll_withDoctorRole_returnsOkWithJson() throws Exception {
        // given
        given(treatmentService.getTodayReceivedList(anyString())).willReturn(List.of());

        // when & then
        mockMvc.perform(get("/doctor/treatment-list/poll")
                .with(user("doctor01").roles("DOCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.body").isArray());
    }

    @Test
    @DisplayName("미인증 — 폴링 엔드포인트 접근 시 로그인 페이지로 리다이렉트")
    void poll_withoutAuthentication_redirectsToLogin() throws Exception {
        // given

        // when & then
        mockMvc.perform(get("/doctor/treatment-list/poll"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("ROLE_STAFF — 폴링 엔드포인트 접근 시 403 반환")
    void poll_withStaffRole_isForbidden() throws Exception {
        // given

        // when & then
        mockMvc.perform(get("/doctor/treatment-list/poll")
                .with(user("staff01").roles("STAFF")))
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
                            .requestMatchers("/doctor/**").hasRole("DOCTOR")
                            .anyRequest().authenticated())
                    .formLogin(form -> form
                            .loginPage("/login")
                            .permitAll())
                    .csrf(csrf -> csrf.disable())
                    .build();
        }

        @Bean
        UserDetailsService userDetailsService() {
            return new InMemoryUserDetailsManager(
                    User.withUsername("doctor01").password("{noop}password").roles("DOCTOR").build(),
                    User.withUsername("staff01").password("{noop}password").roles("STAFF").build());
        }
    }
}
