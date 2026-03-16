package com.smartclinic.hms.admin.staff;

import com.smartclinic.hms.admin.staff.dto.AdminStaffDepartmentOptionResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffFilterOptionResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffFormOptionResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffFormResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffListResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffPageLinkResponse;
import com.smartclinic.hms.admin.staff.dto.UpdateAdminStaffRequest;
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

    private static final String STAFF_CREATED_MESSAGE = "직원이 등록되었습니다.";
    private static final String STAFF_UPDATED_MESSAGE = "직원 정보가 수정되었습니다.";
    private static final String INPUT_CHECK_MESSAGE = "입력값을 확인해주세요.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminStaffService adminStaffService;

    @Test
    @DisplayName("직원 등록 화면을 렌더링한다")
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
    @DisplayName("직원 수정 화면을 렌더링한다")
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

        then(adminStaffService).should().getEditForm(1L);
    }

    @Test
    @DisplayName("직원 목록은 기본 페이징과 pageTitle을 포함해 렌더링한다")
    void list_usesDefaultPagingAndRendersView() throws Exception {
        // given
        AdminStaffListResponse response = createListResponse("ALL", "ALL", "");
        given(adminStaffService.getStaffList(1, 10, null, null, null)).willReturn(response);

        // when
        // then
        mockMvc.perform(get("/admin/staff/list")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/staff-list"))
                .andExpect(request().attribute("model", response))
                .andExpect(request().attribute("pageTitle", "직원 목록"));

        then(adminStaffService).should().getStaffList(1, 10, null, null, null);
    }

    @Test
    @DisplayName("직원 목록은 검색과 필터 파라미터를 서비스에 전달한다")
    void list_passesSearchAndFilterParamsToService() throws Exception {
        // given
        AdminStaffListResponse response = createListResponse("DOCTOR", "ACTIVE", "kim");
        given(adminStaffService.getStaffList(2, 5, "kim", "DOCTOR", "ACTIVE")).willReturn(response);

        // when
        // then
        mockMvc.perform(get("/admin/staff/list")
                        .param("page", "2")
                        .param("size", "5")
                        .param("keyword", "kim")
                        .param("role", "DOCTOR")
                        .param("employmentStatus", "ACTIVE")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/staff-list"))
                .andExpect(request().attribute("model", response))
                .andExpect(request().attribute("pageTitle", "직원 목록"));

        then(adminStaffService).should().getStaffList(2, 5, "kim", "DOCTOR", "ACTIVE");
    }

    @Test
    @DisplayName("직원 등록 성공 시 목록으로 리다이렉트한다")
    void create_success_redirectsToList() throws Exception {
        // given
        given(adminStaffService.createStaff(any())).willReturn(STAFF_CREATED_MESSAGE);

        // when
        // then
        mockMvc.perform(post("/admin/staff/create")
                        .param("username", "staff-new")
                        .param("password", "password123")
                        .param("name", "신규직원")
                        .param("employeeNumber", "S-NEW-001")
                        .param("role", "STAFF")
                        .param("departmentId", "1")
                        .param("active", "true")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("/admin/staff/list")))
                .andExpect(flash().attribute("successMessage", STAFF_CREATED_MESSAGE));
    }

    @Test
    @DisplayName("직원 등록 검증 실패 시 등록 화면을 다시 렌더링한다")
    void create_validationFailure_rendersStaffFormView() throws Exception {
        // given
        AdminStaffFormResponse response = createFormResponse();
        given(adminStaffService.getInputCheckMessage()).willReturn(INPUT_CHECK_MESSAGE);
        given(adminStaffService.getCreateForm(any())).willReturn(response);

        // when
        // then
        mockMvc.perform(post("/admin/staff/create")
                        .param("username", "")
                        .param("password", "123")
                        .param("name", "")
                        .param("employeeNumber", "")
                        .param("role", "")
                        .param("active", "true")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/staff-form"))
                .andExpect(request().attribute("errorMessage", INPUT_CHECK_MESSAGE))
                .andExpect(request().attribute("model", response));
    }

    @Test
    @DisplayName("직원 수정 성공 시 목록으로 리다이렉트한다")
    void update_success_redirectsToList() throws Exception {
        // given
        given(adminStaffService.updateStaff(any(UpdateAdminStaffRequest.class))).willReturn(STAFF_UPDATED_MESSAGE);

        // when
        // then
        mockMvc.perform(post("/admin/staff/update")
                        .param("staffId", "1")
                        .param("name", "수정직원")
                        .param("departmentId", "2")
                        .param("password", "")
                        .param("specialty", "가정의학")
                        .param("availableDays", "MON")
                        .param("availableDays", "WED")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("/admin/staff/list")))
                .andExpect(flash().attribute("successMessage", STAFF_UPDATED_MESSAGE));
    }

    @Test
    @DisplayName("직원 수정 검증 실패 시 수정 화면을 다시 렌더링한다")
    void update_validationFailure_rendersEditFormView() throws Exception {
        // given
        AdminStaffFormResponse response = createEditFormResponse();
        given(adminStaffService.getInputCheckMessage()).willReturn(INPUT_CHECK_MESSAGE);
        given(adminStaffService.getEditForm(any(UpdateAdminStaffRequest.class))).willReturn(response);

        // when
        // then
        mockMvc.perform(post("/admin/staff/update")
                        .param("staffId", "1")
                        .param("name", "")
                        .param("departmentId", "2")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/staff-form"))
                .andExpect(request().attribute("errorMessage", INPUT_CHECK_MESSAGE))
                .andExpect(request().attribute("model", response));

        then(adminStaffService).should().getEditForm(any(UpdateAdminStaffRequest.class));
    }

    private AdminStaffListResponse createListResponse(String selectedRole, String selectedEmploymentStatus, String keyword) {
        return new AdminStaffListResponse(
                List.of(),
                List.of(new AdminStaffFilterOptionResponse("ALL", "전체 역할", "ALL".equals(selectedRole))),
                List.of(new AdminStaffFilterOptionResponse("ALL", "전체 상태", "ALL".equals(selectedEmploymentStatus))),
                List.of(new AdminStaffPageLinkResponse(1, "/admin/staff/list?page=1&size=10&role=ALL&employmentStatus=ALL", true)),
                keyword,
                selectedRole,
                selectedEmploymentStatus,
                0,
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
                "직원 등록",
                "/admin/staff/create",
                "등록하기",
                false,
                null,
                "",
                "",
                "",
                "STAFF",
                "접수 직원",
                null,
                true,
                false,
                "",
                List.of(new AdminStaffFormOptionResponse("STAFF", "접수 직원", true)),
                List.of(new AdminStaffDepartmentOptionResponse(1L, "내과", false)),
                List.of(new AdminStaffFormOptionResponse("true", "재직", true)),
                List.of()
        );
    }

    private AdminStaffFormResponse createEditFormResponse() {
        return new AdminStaffFormResponse(
                "직원 수정",
                "/admin/staff/update",
                "수정하기",
                true,
                1L,
                "doctor01",
                "김의사",
                "D-001",
                "DOCTOR",
                "의사",
                1L,
                true,
                true,
                "내과",
                List.of(new AdminStaffFormOptionResponse("DOCTOR", "의사", true)),
                List.of(new AdminStaffDepartmentOptionResponse(1L, "내과", true)),
                List.of(new AdminStaffFormOptionResponse("true", "재직", true)),
                List.of(new AdminStaffFormOptionResponse("MON", "월요일", true))
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
