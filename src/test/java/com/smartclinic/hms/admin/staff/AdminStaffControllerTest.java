package com.smartclinic.hms.admin.staff;

import com.smartclinic.hms.admin.staff.dto.AdminStaffDepartmentOptionResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffFilterOptionResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffFormOptionResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffFormResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffItemResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffListResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffPageLinkResponse;
import com.smartclinic.hms.admin.staff.dto.UpdateAdminStaffRequest;
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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
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
        value = AdminStaffController.class,
        properties = {
                "spring.mustache.servlet.expose-request-attributes=true",
                "spring.mustache.servlet.allow-request-override=true"
        }
)
@Import(AdminStaffControllerTest.TestSecurityConfig.class)
class AdminStaffControllerTest {

    private static final String STAFF_CREATED_MESSAGE = "Staff created successfully.";
    private static final String STAFF_UPDATED_MESSAGE = "Staff updated successfully.";
    private static final String STAFF_DEACTIVATED_MESSAGE = "Staff deactivated successfully.";
    private static final String INPUT_CHECK_MESSAGE = "Please check the input values.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminStaffService adminStaffService;

    @Test
    @DisplayName("renders create form")
    void newForm_rendersStaffFormView() throws Exception {
        // given
        AdminStaffFormResponse response = createFormResponse();
        given(adminStaffService.getCreateForm()).willReturn(response);

        // when
        // then
        mockMvc.perform(get("/admin/staff/new")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/staff-form"))
                .andExpect(request().attribute("model", response));

        then(adminStaffService).should().getCreateForm();
    }

    @Test
    @DisplayName("renders edit form")
    void detail_rendersEditFormView() throws Exception {
        // given
        AdminStaffFormResponse response = createEditFormResponse();
        given(adminStaffService.getEditForm(1L)).willReturn(response);

        // when
        // then
        mockMvc.perform(get("/admin/staff/detail")
                        .param("staffId", "1")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/staff-form"))
                .andExpect(request().attribute("model", response));
    }

    @Test
    @DisplayName("renders list view with model and pageTitle")
    void list_usesDefaultPagingAndRendersView() throws Exception {
        // given
        AdminStaffListResponse response = createListResponse("ALL", "ALL", "");
        given(adminStaffService.getStaffList(1, 10, null, null, null, "admin")).willReturn(response);

        // when
        // then
        mockMvc.perform(get("/admin/staff/list")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/staff-list"))
                .andExpect(request().attribute("model", response))
                .andExpect(request().attribute("pageTitle", org.hamcrest.Matchers.notNullValue()));
    }

    @Test
    @DisplayName("redirects to list after successful create")
    void create_success_redirectsToList() throws Exception {
        // given
        given(adminStaffService.createStaff(any())).willReturn(STAFF_CREATED_MESSAGE);

        // when
        // then
        mockMvc.perform(post("/admin/staff/create")
                        .param("username", "doctor-new")
                        .param("password", "password123")
                        .param("name", "doctor new")
                        .param("employeeNumber", "D-NEW-001")
                        .param("role", "DOCTOR")
                        .param("departmentId", "1")
                        .param("active", "true")
                        .param("specialty", "family medicine")
                        .param("availableDays", "MON")
                        .param("availableDays", "WED")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("/admin/staff/list")))
                .andExpect(flash().attribute("successMessage", STAFF_CREATED_MESSAGE));
    }

    @Test
    @DisplayName("renders create form again on validation failure")
    void create_validationFailure_rendersStaffFormView() throws Exception {
        // given
        AdminStaffFormResponse response = createDoctorCreateFormResponse();
        given(adminStaffService.getInputCheckMessage()).willReturn(INPUT_CHECK_MESSAGE);
        given(adminStaffService.getCreateForm(any())).willReturn(response);

        // when
        // then
        mockMvc.perform(post("/admin/staff/create")
                        .param("username", "")
                        .param("password", "123")
                        .param("name", "")
                        .param("employeeNumber", "")
                        .param("role", "DOCTOR")
                        .param("active", "true")
                        .param("specialty", "family medicine")
                        .param("availableDays", "MON")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/staff-form"))
                .andExpect(request().attribute("errorMessage", INPUT_CHECK_MESSAGE))
                .andExpect(request().attribute("model", response));
    }

    @Test
    @DisplayName("redirects to list after successful update")
    void update_success_redirectsToList() throws Exception {
        // given
        given(adminStaffService.updateStaff(any(UpdateAdminStaffRequest.class))).willReturn(STAFF_UPDATED_MESSAGE);

        // when
        // then
        mockMvc.perform(post("/admin/staff/update")
                        .param("staffId", "1")
                        .param("name", "updated staff")
                        .param("departmentId", "2")
                        .param("password", "")
                        .param("specialty", "family medicine")
                        .param("availableDays", "MON")
                        .param("availableDays", "WED")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("/admin/staff/list")))
                .andExpect(flash().attribute("successMessage", STAFF_UPDATED_MESSAGE));
    }

    @Test
    @DisplayName("redirects to list after successful deactivate")
    void deactivate_success_redirectsToList() throws Exception {
        // given
        given(adminStaffService.deactivateStaff(2L, "admin")).willReturn(STAFF_DEACTIVATED_MESSAGE);

        // when
        // then
        mockMvc.perform(post("/admin/staff/deactivate")
                        .param("staffId", "2")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("/admin/staff/list")))
                .andExpect(flash().attribute("successMessage", STAFF_DEACTIVATED_MESSAGE));
    }

    private AdminStaffListResponse createListResponse(String selectedRole, String selectedEmploymentStatus, String keyword) {
        return new AdminStaffListResponse(
                List.of(new AdminStaffItemResponse(
                        2L,
                        "Kim Staff",
                        "staff01",
                        "S-001",
                        "STAFF",
                        "Staff",
                        "bg-purple-100 text-purple-800",
                        "Internal Medicine",
                        true,
                        "Active",
                        "bg-green-100 text-green-800",
                        "/admin/staff/detail?staffId=2",
                        true,
                        ""
                )),
                List.of(new AdminStaffFilterOptionResponse("ALL", "All Roles", "ALL".equals(selectedRole))),
                List.of(new AdminStaffFilterOptionResponse("ALL", "All Status", "ALL".equals(selectedEmploymentStatus))),
                List.of(new AdminStaffPageLinkResponse(1, "/admin/staff/list?page=1&size=10&role=ALL&employmentStatus=ALL", true)),
                keyword,
                selectedRole,
                selectedEmploymentStatus,
                1,
                1,
                10,
                1,
                false,
                false,
                "",
                ""
        );
    }

    private AdminStaffFormResponse createFormResponse() {
        return new AdminStaffFormResponse(
                "Staff Create",
                "/admin/staff/create",
                "Create",
                false,
                null,
                "",
                "",
                "",
                "STAFF",
                "Staff",
                null,
                true,
                false,
                "",
                List.of(new AdminStaffFormOptionResponse("STAFF", "Staff", true)),
                List.of(new AdminStaffDepartmentOptionResponse(1L, "Internal Medicine", false)),
                List.of(new AdminStaffFormOptionResponse("true", "Active", true)),
                List.of()
        );
    }

    private AdminStaffFormResponse createDoctorCreateFormResponse() {
        return new AdminStaffFormResponse(
                "Staff Create",
                "/admin/staff/create",
                "Create",
                false,
                null,
                "",
                "",
                "",
                "DOCTOR",
                "Doctor",
                null,
                true,
                true,
                "family medicine",
                List.of(new AdminStaffFormOptionResponse("DOCTOR", "Doctor", true)),
                List.of(new AdminStaffDepartmentOptionResponse(1L, "Family Medicine", false)),
                List.of(new AdminStaffFormOptionResponse("true", "Active", true)),
                List.of(new AdminStaffFormOptionResponse("MON", "Monday", true))
        );
    }

    private AdminStaffFormResponse createEditFormResponse() {
        return new AdminStaffFormResponse(
                "Staff Edit",
                "/admin/staff/update",
                "Save",
                true,
                1L,
                "doctor01",
                "Kim Doctor",
                "D-001",
                "DOCTOR",
                "Doctor",
                1L,
                true,
                true,
                "internal medicine",
                List.of(new AdminStaffFormOptionResponse("DOCTOR", "Doctor", true)),
                List.of(new AdminStaffDepartmentOptionResponse(1L, "Internal Medicine", true)),
                List.of(new AdminStaffFormOptionResponse("true", "Active", true)),
                List.of(new AdminStaffFormOptionResponse("MON", "Monday", true))
        );
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