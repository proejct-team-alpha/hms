package com.smartclinic.hms.admin.reservation;

import com.smartclinic.hms.admin.reservation.dto.AdminReservationListResponse;
import com.smartclinic.hms.admin.reservation.dto.AdminReservationPageLinkResponse;
import com.smartclinic.hms.admin.reservation.dto.AdminReservationStatusOptionResponse;
import com.smartclinic.hms.common.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(
        value = AdminReservationController.class,
        properties = {
                "spring.mustache.servlet.expose-request-attributes=true",
                "spring.mustache.servlet.allow-request-override=true"
        }
)
@Import(AdminReservationControllerTest.TestSecurityConfig.class)
class AdminReservationControllerTest {

    private static final String CANCEL_SUCCESS_MESSAGE = "\uC608\uC57D\uC774 \uCDE8\uC18C\uB418\uC5C8\uC2B5\uB2C8\uB2E4.";
    private static final String INVALID_STATUS_MESSAGE = "\uCDE8\uC18C\uD560 \uC218 \uC5C6\uB294 \uC0C1\uD0DC\uC785\uB2C8\uB2E4.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminReservationService adminReservationService;

    @Test
    @DisplayName("list uses default paging and renders reservation list view")
    void list_usesDefaultPagingAndRendersView() throws Exception {
        // given
        AdminReservationListResponse viewModel = new AdminReservationListResponse(
                List.of(),
                List.of(new AdminReservationStatusOptionResponse("ALL", "ALL", "/admin/reservation/list?page=1&size=10&status=ALL", true)),
                List.of(new AdminReservationPageLinkResponse(1, "/admin/reservation/list?page=1&size=10&status=ALL", true)),
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

        // when // then
        mockMvc.perform(get("/admin/reservation/list")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reservation-list"))
                .andExpect(request().attribute("model", viewModel));

        then(adminReservationService).should().getReservationList(1, 10, null);
    }

    @Test
    @DisplayName("list passes request params to service")
    void list_passesRequestParamsToService() throws Exception {
        // given
        AdminReservationListResponse viewModel = new AdminReservationListResponse(
                List.of(), List.of(), List.of(), "RESERVED", 0, 2, 5, 0,
                true, false,
                "/admin/reservation/list?page=1&size=5&status=RESERVED",
                ""
        );
        given(adminReservationService.getReservationList(2, 5, "RESERVED")).willReturn(viewModel);

        // when // then
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

    @Test
    @DisplayName("cancel success redirects with success flash message")
    void cancel_success_redirectsWithSuccessMessage() throws Exception {
        // when // then
        mockMvc.perform(post("/admin/reservation/cancel")
                        .param("reservationId", "100")
                        .param("page", "2")
                        .param("size", "10")
                        .param("status", "RECEIVED")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("/admin/reservation/list?page=2&size=10&status=RECEIVED")))
                .andExpect(flash().attribute("successMessage", CANCEL_SUCCESS_MESSAGE));

        then(adminReservationService).should().cancelReservation(100L);
    }

    @Test
    @DisplayName("cancel failure redirects with error flash message")
    void cancel_failure_redirectsWithErrorMessage() throws Exception {
        // given
        willThrow(new CustomException("INVALID_STATUS_TRANSITION", INVALID_STATUS_MESSAGE, HttpStatus.CONFLICT))
                .given(adminReservationService).cancelReservation(100L);

        // when // then
        mockMvc.perform(post("/admin/reservation/cancel")
                        .param("reservationId", "100")
                        .param("page", "2")
                        .param("size", "10")
                        .param("status", "COMPLETED")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("/admin/reservation/list?page=2&size=10&status=COMPLETED")))
                .andExpect(flash().attribute("errorMessage", INVALID_STATUS_MESSAGE));

        then(adminReservationService).should().cancelReservation(100L);
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
