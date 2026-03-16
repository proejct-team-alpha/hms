package com.smartclinic.hms.admin.staff;

import com.smartclinic.hms.admin.staff.dto.AdminStaffDepartmentOptionResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffFormOptionResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffFormResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffFilterOptionResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffListResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffPageLinkResponse;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.hamcrest.Matchers.containsString;

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
    @DisplayName("직원 목록 기본 페이징으로 화면을 렌더링한다")
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
                .andExpect(request().attribute("model", response));

        then(adminStaffService).should().getStaffList(1, 10, null, null, null);
    }

    @Test
    @DisplayName("직원 목록 검색과 필터 파라미터를 서비스에 전달한다")
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
                .andExpect(request().attribute("model", response));

        then(adminStaffService).should().getStaffList(2, 5, "kim", "DOCTOR", "ACTIVE");
    }

    @Test
    @DisplayName("직원 등록 성공 시 목록으로 리다이렉트한다")
    void create_success_redirectsToList() throws Exception {
        // given
        given(adminStaffService.createStaff(org.mockito.ArgumentMatchers.any())).willReturn(STAFF_CREATED_MESSAGE);

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
    @DisplayName("직원 등록 검증 실패 시 등록 화면으로 다시 렌더링한다")
    void create_validationFailure_rendersStaffFormView() throws Exception {
        // given
        AdminStaffFormResponse response = createFormResponse();
        given(adminStaffService.getCreateForm(org.mockito.ArgumentMatchers.any())).willReturn(response);

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
                .andExpect(request().attribute("errorMessage", "입력값을 확인해주세요."))
                .andExpect(request().attribute("model", response));
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
                "",
                "",
                "",
                "STAFF",
                null,
                true,
                List.of(new AdminStaffFormOptionResponse("STAFF", "접수 직원", true)),
                List.of(new AdminStaffDepartmentOptionResponse(1L, "내과", false)),
                List.of(new AdminStaffFormOptionResponse("true", "재직", true))
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
