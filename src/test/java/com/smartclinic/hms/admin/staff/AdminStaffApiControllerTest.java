package com.smartclinic.hms.admin.staff;

import com.smartclinic.hms.admin.staff.dto.UpdateAdminStaffApiRequest;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminStaffApiController.class)
@Import(AdminStaffApiControllerTest.TestSecurityConfig.class)
class AdminStaffApiControllerTest {

    private static final String STAFF_UPDATED_MESSAGE = "직원 정보를 수정했습니다.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminStaffService adminStaffService;

    @Test
    @DisplayName("admin can update staff via api")
    void updateStaff_success() throws Exception {
        // given
        UpdateAdminStaffApiRequest request = new UpdateAdminStaffApiRequest(
                "수정직원",
                2L,
                "",
                "가정의학과",
                List.of("MON", "WED")
        );
        UpdateAdminStaffApiResponse response = new UpdateAdminStaffApiResponse(
                10L,
                "doctor01",
                "D-001",
                "수정직원",
                "DOCTOR",
                2L,
                "가정의학과",
                true,
                "가정의학과",
                List.of("MON", "WED"),
                STAFF_UPDATED_MESSAGE
        );
        given(adminStaffService.updateStaff(any())).willReturn(STAFF_UPDATED_MESSAGE);
        given(adminStaffService.getUpdateApiResponse(10L, STAFF_UPDATED_MESSAGE)).willReturn(response);

        // when
        // then
        mockMvc.perform(post("/api/staff/10")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "수정직원",
                                  "departmentId": 2,
                                  "password": "",
                                  "specialty": "가정의학과",
                                  "availableDays": ["MON", "WED"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.body.staffId").value(10))
                .andExpect(jsonPath("$.body.name").value("수정직원"))
                .andExpect(jsonPath("$.body.role").value("DOCTOR"))
                .andExpect(jsonPath("$.body.departmentId").value(2))
                .andExpect(jsonPath("$.body.message").value(STAFF_UPDATED_MESSAGE));

        then(adminStaffService).should().updateStaff(any());
        then(adminStaffService).should().getUpdateApiResponse(10L, STAFF_UPDATED_MESSAGE);
    }

    @Test
    @DisplayName("non-admin cannot update staff via api")
    void updateStaff_forbiddenWhenNotAdmin() throws Exception {
        // given
        UpdateAdminStaffApiRequest request = new UpdateAdminStaffApiRequest(
                "수정직원",
                2L,
                "",
                null,
                List.of()
        );

        // when
        // then
        mockMvc.perform(post("/api/staff/10")
                        .with(user("staff").roles("STAFF"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "수정직원",
                                  "departmentId": 2,
                                  "password": "",
                                  "availableDays": []
                                }
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminStaffService);
    }

    @Test
    @DisplayName("returns 404 when staff does not exist")
    void updateStaff_notFound() throws Exception {
        // given
        UpdateAdminStaffApiRequest request = new UpdateAdminStaffApiRequest(
                "수정직원",
                2L,
                "",
                null,
                List.of()
        );
        given(adminStaffService.updateStaff(any()))
                .willThrow(CustomException.notFound("직원을 찾을 수 없습니다."));

        // when
        // then
        mockMvc.perform(post("/api/staff/999")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "수정직원",
                                  "departmentId": 2,
                                  "password": "",
                                  "availableDays": []
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("직원을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("returns 400 when request body is invalid")
    void updateStaff_validationFailure() throws Exception {
        // given
        UpdateAdminStaffApiRequest request = new UpdateAdminStaffApiRequest(
                "",
                2L,
                "",
                null,
                List.of()
        );

        // when
        // then
        mockMvc.perform(post("/api/staff/10")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "",
                                  "departmentId": 2,
                                  "password": "",
                                  "availableDays": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/staff/**").hasRole("ADMIN")
                            .anyRequest().authenticated())
                    .formLogin(form -> form.disable())
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
