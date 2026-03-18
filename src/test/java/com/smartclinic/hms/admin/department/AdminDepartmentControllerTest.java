package com.smartclinic.hms.admin.department;

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

@WebMvcTest(AdminDepartmentController.class)
@Import(AdminDepartmentControllerTest.TestSecurityConfig.class)
class AdminDepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminDepartmentService adminDepartmentService;

    // ── GET /admin/department/list ───────────────────────────────────────────

    @Test
    @DisplayName("ROLE_ADMIN — 진료과 목록 페이지 접근 시 200 반환")
    void departmentList_withAdminRole_returnsOk() throws Exception {
        given(adminDepartmentService.getDepartmentList()).willReturn(List.of());

        mockMvc.perform(get("/admin/department/list")
                        .with(user("admin01").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/department-list"));
    }

    @Test
    @DisplayName("미인증 — 진료과 목록 접근 시 로그인 페이지로 리다이렉트")
    void departmentList_withoutAuthentication_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/department/list"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("ROLE_NURSE — 진료과 목록 접근 시 403 반환")
    void departmentList_withNurseRole_isForbidden() throws Exception {
        mockMvc.perform(get("/admin/department/list")
                        .with(user("nurse01").roles("NURSE")))
                .andExpect(status().isForbidden());
    }

    // ── POST /admin/department/form ──────────────────────────────────────────

    @Test
    @DisplayName("ROLE_ADMIN — 진료과 등록 후 목록으로 리다이렉트")
    void createDepartment_withAdminRole_redirectsToList() throws Exception {
        mockMvc.perform(post("/admin/department/form")
                        .param("name", "신경외과")
                        .with(user("admin01").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrlPattern("/admin/department/list*"));

        then(adminDepartmentService).should().createDepartment("신경외과");
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
                    User.withUsername("nurse01").password("{noop}password").roles("NURSE").build());
        }
    }
}
