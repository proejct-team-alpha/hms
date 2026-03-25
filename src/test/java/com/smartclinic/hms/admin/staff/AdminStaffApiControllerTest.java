package com.smartclinic.hms.admin.staff;

import com.smartclinic.hms.admin.staff.dto.UpdateAdminStaffApiResponse;
import com.smartclinic.hms.common.exception.CustomException;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminStaffApiController.class)
@Import(AdminStaffApiControllerTest.TestSecurityConfig.class)
class AdminStaffApiControllerTest {

    private static final String STAFF_UPDATED_MESSAGE = "staff updated";
    private static final String SELF_UPDATE_BLOCKED_MESSAGE = "self account cannot be deactivated";
    private static final String INACTIVE_UPDATE_NOT_ALLOWED_MESSAGE = "\uBE44\uD65C\uC131\uD654\uB41C \uC9C1\uC6D0\uC740 \uC218\uC815\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4.";
    private static final String STAFF_NOT_FOUND_MESSAGE = "staff not found";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminStaffService adminStaffService;

    @Test
    @DisplayName("updateStaff returns ok response for admin")
    void updateStaff_success() throws Exception {
        // given
        UpdateAdminStaffApiResponse response = new UpdateAdminStaffApiResponse(
                10L,
                "doctor01",
                "D-001",
                "updated staff",
                "DOCTOR",
                2L,
                "family medicine",
                true,
                "2026-03-31T18:00",
                List.of("MON", "WED"),
                STAFF_UPDATED_MESSAGE
        );
        given(adminStaffService.updateStaff(any(), eq("admin"))).willReturn(STAFF_UPDATED_MESSAGE);
        given(adminStaffService.getUpdateApiResponse(10L, STAFF_UPDATED_MESSAGE)).willReturn(response);

        // when
        mockMvc.perform(post("/admin/api/staff/10")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestJson("updated staff", 2L, "", true, "2026-03-31T18:00", List.of("MON", "WED"))))
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.body.staffId").value(10))
                .andExpect(jsonPath("$.body.name").value("updated staff"))
                .andExpect(jsonPath("$.body.role").value("DOCTOR"))
                .andExpect(jsonPath("$.body.departmentId").value(2))
                .andExpect(jsonPath("$.body.retiredAt").value("2026-03-31T18:00"))
                .andExpect(jsonPath("$.body.message").value(STAFF_UPDATED_MESSAGE));

        then(adminStaffService).should().updateStaff(any(), eq("admin"));
        then(adminStaffService).should().getUpdateApiResponse(10L, STAFF_UPDATED_MESSAGE);
    }

    @Test
    @DisplayName("updateStaff forwards retiredAtDate and retiredAtHour")
    void updateStaff_forwardsRetiredAtDateAndHour() throws Exception {
        // given
        UpdateAdminStaffApiResponse response = new UpdateAdminStaffApiResponse(
                10L,
                "doctor01",
                "D-001",
                "updated staff",
                "DOCTOR",
                2L,
                "family medicine",
                true,
                "2026-03-31T18:00",
                List.of("MON", "WED"),
                STAFF_UPDATED_MESSAGE
        );
        given(adminStaffService.updateStaff(any(), eq("admin"))).willReturn(STAFF_UPDATED_MESSAGE);
        given(adminStaffService.getUpdateApiResponse(10L, STAFF_UPDATED_MESSAGE)).willReturn(response);

        // when
        mockMvc.perform(post("/admin/api/staff/10")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestJson("updated staff", 2L, "", true, null, "2026-03-31", "18", List.of("MON", "WED"))))
                // then
                .andExpect(status().isOk());

        then(adminStaffService).should().updateStaff(
                argThat(request -> request.retiredAt() == null
                        && "2026-03-31".equals(request.retiredAtDate())
                        && "18".equals(request.retiredAtHour())),
                eq("admin"));
    }

    @Test
    @DisplayName("updateStaff is forbidden for non-admin")
    void updateStaff_forbiddenWhenNotAdmin() throws Exception {
        // when
        mockMvc.perform(post("/admin/api/staff/10")
                        .with(user("staff").roles("STAFF"))
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestJson("updated staff", 2L, "", true, null, List.of())))
                // then
                .andExpect(status().isForbidden());

        then(adminStaffService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("updateStaff returns bad request for self deactivate attempt")
    void updateStaff_selfDeactivate_returnsBadRequest() throws Exception {
        // given
        given(adminStaffService.updateStaff(any(), eq("doctor01")))
                .willThrow(CustomException.badRequest("VALIDATION_ERROR", SELF_UPDATE_BLOCKED_MESSAGE));

        // when
        mockMvc.perform(post("/admin/api/staff/10")
                        .with(user("doctor01").roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestJson("updated staff", 2L, "", false, null, List.of())))
                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.msg", containsString("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.msg", containsString(SELF_UPDATE_BLOCKED_MESSAGE)));
    }

    @Test
    @DisplayName("updateStaff returns bad request for self retiredAt change")
    void updateStaff_selfRetiredAtChange_returnsBadRequest() throws Exception {
        // given
        given(adminStaffService.updateStaff(any(), eq("doctor01")))
                .willThrow(CustomException.badRequest("VALIDATION_ERROR", SELF_UPDATE_BLOCKED_MESSAGE));

        // when
        mockMvc.perform(post("/admin/api/staff/10")
                        .with(user("doctor01").roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestJson("updated staff", 2L, "", true, "2026-03-31T18:00", List.of())))
                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.msg", containsString("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.msg", containsString(SELF_UPDATE_BLOCKED_MESSAGE)));
    }

    @Test
    @DisplayName("updateStaff returns bad request for inactive staff")
    void updateStaff_inactiveStaffUpdateAttempt_returnsBadRequest() throws Exception {
        // given
        given(adminStaffService.updateStaff(any(), eq("admin")))
                .willThrow(CustomException.badRequest("VALIDATION_ERROR", INACTIVE_UPDATE_NOT_ALLOWED_MESSAGE));

        // when
        mockMvc.perform(post("/admin/api/staff/20")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestJson("inactive staff", 2L, "", true, null, List.of())))
                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.msg", containsString("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.msg", containsString(INACTIVE_UPDATE_NOT_ALLOWED_MESSAGE)));
    }

    @Test
    @DisplayName("updateStaff returns not found when staff does not exist")
    void updateStaff_notFound() throws Exception {
        // given
        given(adminStaffService.updateStaff(any(), eq("admin")))
                .willThrow(CustomException.notFound(STAFF_NOT_FOUND_MESSAGE));

        // when
        mockMvc.perform(post("/admin/api/staff/999")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestJson("updated staff", 2L, "", true, null, List.of())))
                // then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.msg", containsString("RESOURCE_NOT_FOUND")))
                .andExpect(jsonPath("$.msg", containsString(STAFF_NOT_FOUND_MESSAGE)));
    }

    @Test
    @DisplayName("updateStaff returns bad request for validation failure")
    void updateStaff_validationFailure() throws Exception {
        // when
        mockMvc.perform(post("/admin/api/staff/10")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestJson("", 2L, "", true, null, List.of())))
                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.msg", containsString("VALIDATION_ERROR")));

        then(adminStaffService).shouldHaveNoInteractions();
    }

    private String requestJson(
            String name,
            Long departmentId,
            String password,
            boolean active,
            String retiredAt,
            List<String> availableDays
    ) {
        return requestJson(name, departmentId, password, active, retiredAt, null, null, availableDays);
    }

    private String requestJson(
            String name,
            Long departmentId,
            String password,
            boolean active,
            String retiredAt,
            String retiredAtDate,
            String retiredAtHour,
            List<String> availableDays
    ) {
        String departmentIdJson = departmentId == null ? "null" : departmentId.toString();
        String retiredAtJson = retiredAt == null ? "null" : "\"" + retiredAt + "\"";
        String retiredAtDateJson = retiredAtDate == null ? "null" : "\"" + retiredAtDate + "\"";
        String retiredAtHourJson = retiredAtHour == null ? "null" : "\"" + retiredAtHour + "\"";
        String daysJson = availableDays.stream()
                .map(day -> "\"" + day + "\"")
                .collect(java.util.stream.Collectors.joining(", "));

        return """
                {
                  \"name\": \"%s\",
                  \"departmentId\": %s,
                  \"password\": \"%s\",
                  \"active\": %s,
                  \"retiredAt\": %s,
                  \"retiredAtDate\": %s,
                  \"retiredAtHour\": %s,
                  \"availableDays\": [%s]
                }
                """.formatted(name, departmentIdJson, password, Boolean.toString(active), retiredAtJson, retiredAtDateJson, retiredAtHourJson, daysJson);
    }

    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/admin/api/staff/**").hasRole("ADMIN")
                            .anyRequest().authenticated())
                    .formLogin(form -> form.disable())
                    .csrf(csrf -> csrf.disable())
                    .build();
        }

        @Bean
        UserDetailsService userDetailsService() {
            return new InMemoryUserDetailsManager(
                    User.withUsername("admin").password("{noop}password").roles("ADMIN").build(),
                    User.withUsername("staff").password("{noop}password").roles("STAFF").build(),
                    User.withUsername("doctor01").password("{noop}password").roles("ADMIN").build());
        }
    }
}
