package com.smartclinic.hms.admin.reservation;

import com.smartclinic.hms.admin.reservation.dto.AdminReservationListView;
import com.smartclinic.hms.admin.reservation.dto.AdminReservationPageLink;
import com.smartclinic.hms.admin.reservation.dto.AdminReservationStatusOption;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminReservationController.class)
@Import(AdminReservationControllerTest.TestSecurityConfig.class)
class AdminReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminReservationService adminReservationService;

    @Test
    @DisplayName("기본 파라미터(page=1, size=10)로 예약 목록을 조회한다")
    void list_usesDefaultPagingAndRendersView() throws Exception {
        // given
        AdminReservationListView viewModel = new AdminReservationListView(
                List.of(),
                List.of(new AdminReservationStatusOption("ALL", "전체", "/admin/reservation/list?page=1&size=10&status=ALL", true)),
                List.of(new AdminReservationPageLink(1, "/admin/reservation/list?page=1&size=10&status=ALL", true)),
                "ALL",
                0,
                1,
                10,
                1,
                false,
                false,
                "",
                ""
        );
        given(adminReservationService.getReservationList(1, 10, null)).willReturn(viewModel);

        // when
        // then
        mockMvc.perform(get("/admin/reservation/list")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reservation-list"))
                .andExpect(request().attribute("model", viewModel));

        then(adminReservationService).should().getReservationList(1, 10, null);
    }

    @Test
    @DisplayName("전달된 page, size, status 파라미터를 서비스로 전달한다")
    void list_passesRequestParamsToService() throws Exception {
        // given
        AdminReservationListView viewModel = new AdminReservationListView(
                List.of(), List.of(), List.of(), "RESERVED", 0, 2, 5, 0,
                true, false,
                "/admin/reservation/list?page=1&size=5&status=RESERVED",
                ""
        );
        given(adminReservationService.getReservationList(2, 5, "RESERVED")).willReturn(viewModel);

        // when
        // then
        mockMvc.perform(get("/admin/reservation/list")
                .param("page", "2")
                .param("size", "5")
                .param("status", "RESERVED")
                .with(user("admin").roles("ADMIN"))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reservation-list"))
                .andExpect(request().attribute("model", viewModel));

        then(adminReservationService).should().getReservationList(2, 5, "RESERVED");
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
                    .formLogin(form -> form
                            .loginPage("/login")
                            .permitAll())
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
