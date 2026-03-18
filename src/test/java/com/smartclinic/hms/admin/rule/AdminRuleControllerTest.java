package com.smartclinic.hms.admin.rule;

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

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminRuleController.class)
@Import(AdminRuleControllerTest.TestSecurityConfig.class)
class AdminRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminRuleService adminRuleService;

    // ── GET /admin/rule/list ─────────────────────────────────────────────────

    @Test
    @DisplayName("ROLE_ADMIN — 병원 규칙 목록 접근 시 200 반환")
    void ruleList_withAdminRole_returnsOk() throws Exception {
        given(adminRuleService.getRuleList()).willReturn(List.of());

        mockMvc.perform(get("/admin/rule/list")
                        .with(user("admin01").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/rule-list"));
    }

    @Test
    @DisplayName("미인증 — 병원 규칙 목록 접근 시 로그인 페이지로 리다이렉트")
    void ruleList_withoutAuthentication_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/rule/list"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("ROLE_DOCTOR — 병원 규칙 목록 접근 시 403 반환")
    void ruleList_withDoctorRole_isForbidden() throws Exception {
        mockMvc.perform(get("/admin/rule/list")
                        .with(user("doctor01").roles("DOCTOR")))
                .andExpect(status().isForbidden());
    }

    // ── POST /admin/rule/form ────────────────────────────────────────────────

    @Test
    @DisplayName("ROLE_ADMIN — 규칙 등록 후 목록으로 리다이렉트")
    void createRule_withAdminRole_redirectsToList() throws Exception {
        mockMvc.perform(post("/admin/rule/form")
                        .param("title", "응급 절차")
                        .param("content", "응급 시 이렇게 하세요")
                        .param("category", "EMERGENCY")
                        .with(user("admin01").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrlPattern("/admin/rule/list*"));

        then(adminRuleService).should().createRule("응급 절차", "응급 시 이렇게 하세요", "EMERGENCY");
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
                            .requestMatchers("/admin/**").hasRole("ADMIN")
                            .anyRequest().authenticated())
                    .formLogin(form -> form.loginPage("/login").permitAll())
                    .csrf(csrf -> csrf.disable())
                    .build();
        }

        @Bean
        UserDetailsService userDetailsService() {
            return new InMemoryUserDetailsManager(
                    User.withUsername("admin01").password("{noop}password").roles("ADMIN").build(),
                    User.withUsername("doctor01").password("{noop}password").roles("DOCTOR").build());
        }
    }
}
