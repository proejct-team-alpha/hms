package com.smartclinic.hms.nurse;

import com.smartclinic.hms.item.ItemManagerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NurseReceptionController.class)
@Import(NurseReceptionControllerTest.TestSecurityConfig.class)
class NurseReceptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NurseService nurseService;

    @MockitoBean
    private ItemManagerService itemManagerService;

    // ── GET /nurse/reception-list ────────────────────────────────────────────

    @Test
    @DisplayName("ROLE_NURSE — 예약 현황 목록 접근 시 200 반환")
    void receptionList_withNurseRole_returnsOk() throws Exception {
        given(nurseService.getReceptionPage(any(), any(Integer.class)))
                .willReturn(new PageImpl<>(List.of()));
        given(nurseService.getStatusFilters(any())).willReturn(List.of());

        mockMvc.perform(get("/nurse/reception-list")
                        .with(user("nurse01").roles("NURSE"))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("미인증 — 예약 현황 목록 접근 시 로그인 페이지로 리다이렉트")
    void receptionList_withoutAuthentication_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/nurse/reception-list"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("ROLE_STAFF — 예약 현황 목록 접근 시 403 반환")
    void receptionList_withStaffRole_isForbidden() throws Exception {
        mockMvc.perform(get("/nurse/reception-list")
                        .with(user("staff01").roles("STAFF")))
                .andExpect(status().isForbidden());
    }

    // ── POST /nurse/item/use ─────────────────────────────────────────────────

    @Test
    @DisplayName("ROLE_NURSE — 유효한 수량 출고 요청 시 200과 JSON 반환")
    void useItem_withValidAmount_returnsOkWithJson() throws Exception {
        given(itemManagerService.useItem(anyLong(), any(Integer.class), any())).willReturn(30);
        given(itemManagerService.getUsageLogs(anyLong())).willReturn(List.of());

        mockMvc.perform(post("/nurse/item/use")
                        .param("id", "1")
                        .param("amount", "5")
                        .param("reservationId", "10")
                        .with(user("nurse01").roles("NURSE"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(30));
    }

    @Test
    @DisplayName("ROLE_NURSE — 수량 0 출고 요청 시 400 반환")
    void useItem_withZeroAmount_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/nurse/item/use")
                        .param("id", "1")
                        .param("amount", "0")
                        .with(user("nurse01").roles("NURSE"))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
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
                            .requestMatchers("/nurse/**").hasAnyRole("NURSE", "ADMIN")
                            .anyRequest().authenticated())
                    .formLogin(form -> form.loginPage("/login").permitAll())
                    .csrf(csrf -> csrf.disable())
                    .build();
        }

        @Bean
        UserDetailsService userDetailsService() {
            return new InMemoryUserDetailsManager(
                    User.withUsername("nurse01").password("{noop}password").roles("NURSE").build(),
                    User.withUsername("staff01").password("{noop}password").roles("STAFF").build());
        }
    }
}
