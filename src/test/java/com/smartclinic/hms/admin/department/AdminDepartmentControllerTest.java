package com.smartclinic.hms.admin.department;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.smartclinic.hms.admin.department.dto.AdminDepartmentDetailResponse;
import com.smartclinic.hms.admin.department.dto.AdminDepartmentItemResponse;
import com.smartclinic.hms.admin.department.dto.AdminDepartmentListResponse;
import com.smartclinic.hms.admin.department.dto.AdminDepartmentPageLinkResponse;
import com.smartclinic.hms.common.util.SsrValidationViewSupport;
import com.smartclinic.hms.common.exception.CustomException;
import com.smartclinic.hms.domain.Department;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = AdminDepartmentController.class, properties = {
                "spring.mustache.servlet.expose-request-attributes=true",
                "spring.mustache.servlet.allow-request-override=true"
})
@Import(AdminDepartmentControllerTest.TestSecurityConfig.class)
class AdminDepartmentControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private AdminDepartmentService adminDepartmentService;

        @Test
        @DisplayName("list renders with default paging")
        void list_usesDefaultPagingAndRendersView() throws Exception {
                // given
                AdminDepartmentListResponse response = createListResponse();
                given(adminDepartmentService.getDepartmentList(1, 10)).willReturn(response);

                // when
                // then
                mockMvc.perform(get("/admin/department/list")
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin/department-list"))
                                .andExpect(request().attribute("model", response));

                then(adminDepartmentService).should().getDepartmentList(1, 10);
        }

        @Test
        @DisplayName("list passes request params to service")
        void list_passesRequestParamsToService() throws Exception {
                // given
                AdminDepartmentListResponse response = createListResponse();
                given(adminDepartmentService.getDepartmentList(2, 5)).willReturn(response);

                // when
                // then
                mockMvc.perform(get("/admin/department/list")
                                .param("page", "2")
                                .param("size", "5")
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin/department-list"))
                                .andExpect(request().attribute("model", response));

                then(adminDepartmentService).should().getDepartmentList(2, 5);
        }

        @Test
        @DisplayName("list renders pagination ui")
        void list_rendersPaginationUi() throws Exception {
                // given
                Department department = Department.create("Dept", true);
                ReflectionTestUtils.setField(department, "id", 12L);
                AdminDepartmentItemResponse item = new AdminDepartmentItemResponse(department);
                AdminDepartmentListResponse response = new AdminDepartmentListResponse(
                                List.of(item),
                                List.of(
                                                new AdminDepartmentPageLinkResponse(1,
                                                                "/admin/department/list?page=1&size=5", false),
                                                new AdminDepartmentPageLinkResponse(2,
                                                                "/admin/department/list?page=2&size=5", true),
                                                new AdminDepartmentPageLinkResponse(3,
                                                                "/admin/department/list?page=3&size=5", false)),
                                12,
                                2,
                                5,
                                3,
                                true,
                                true,
                                true,
                                "/admin/department/list?page=1&size=5",
                                "/admin/department/list?page=3&size=5");
                given(adminDepartmentService.getDepartmentList(2, 5)).willReturn(response);

                // when
                // then
                mockMvc.perform(get("/admin/department/list")
                                .param("page", "2")
                                .param("size", "5")
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(content().string(containsString("Dept")))
                                .andExpect(content().string(containsString("/admin/department/detail?departmentId=12")))
                                .andExpect(content().string(containsString(">1</a>")))
                                .andExpect(content().string(containsString(">3</a>")));
        }

        @Test
        @DisplayName("list renders empty state")
        void list_rendersEmptyStateWhenNoDepartments() throws Exception {
                // given
                AdminDepartmentListResponse response = createListResponse();
                given(adminDepartmentService.getDepartmentList(1, 10)).willReturn(response);

                // when
                // then
                mockMvc.perform(get("/admin/department/list")
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(content().string(containsString("0 / 0")));
        }

        @Test
        @DisplayName("detail renders update and status actions")
        void detail_rendersDepartmentDetailView() throws Exception {
                // given
                AdminDepartmentDetailResponse response = new AdminDepartmentDetailResponse(
                                3L,
                                "Dept",
                                true,
                                "ACTIVE",
                                "px-2 py-1 text-xs font-medium rounded-full bg-green-100 text-green-700",
                                false,
                                true,
                                "/admin/department/update",
                                "/admin/department/activate",
                                "/admin/department/deactivate");
                given(adminDepartmentService.getDepartmentDetail(3L)).willReturn(response);

                // when
                // then
                mockMvc.perform(get("/admin/department/detail")
                                .param("departmentId", "3")
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin/department-detail"))
                                .andExpect(request().attribute("model", response))
                                .andExpect(request().attribute("editName", "Dept"))
                                .andExpect(content().string(containsString("Dept")))
                                .andExpect(content().string(containsString("/admin/department/update")))
                                .andExpect(content().string(containsString("/admin/department/deactivate")));
        }

        @Test
        @DisplayName("detail returns 404 when target is missing")
        void detail_returns404WhenDepartmentMissing() throws Exception {
                // given
                given(adminDepartmentService.getDepartmentDetail(99L))
                                .willThrow(CustomException.notFound("not found"));

                // when
                // then
                mockMvc.perform(get("/admin/department/detail")
                                .param("departmentId", "99")
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf()))
                                .andExpect(status().isNotFound())
                                .andExpect(view().name("error/404"))
                                .andExpect(request().attribute("errorMessage", "not found"));
        }

        @Test
        @DisplayName("update redirects to detail on success")
        void update_redirectsToDetailWithSuccessMessage() throws Exception {
                // given
                given(adminDepartmentService.updateDepartmentName(5L, "Dept")).willReturn("updated");

                // when
                // then
                mockMvc.perform(post("/admin/department/update")
                                .param("departmentId", "5")
                                .param("name", "Dept")
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/department/detail?departmentId=5"))
                                .andExpect(flash().attribute("successMessage", "updated"));

                then(adminDepartmentService).should().updateDepartmentName(5L, "Dept");
        }

        @Test
        @DisplayName("update validation failure renders detail view with field error")
        void update_validationFailure_rendersDetailView() throws Exception {
                // given
                AdminDepartmentDetailResponse response = new AdminDepartmentDetailResponse(
                                5L,
                                "기존 진료과",
                                true,
                                "ACTIVE",
                                "px-2 py-1 text-xs font-medium rounded-full bg-green-100 text-green-700",
                                false,
                                true,
                                "/admin/department/update",
                                "/admin/department/activate",
                                "/admin/department/deactivate");
                given(adminDepartmentService.getDepartmentDetail(5L)).willReturn(response);

                // when
                // then
                mockMvc.perform(post("/admin/department/update")
                                .param("departmentId", "5")
                                .param("name", "   ")
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/department-detail"))
                .andExpect(request().attribute("model", response))
                .andExpect(request().attribute("errorMessage", SsrValidationViewSupport.INPUT_CHECK_MESSAGE))
                .andExpect(request().attribute("editName", "   "))
                .andExpect(request().attribute("nameError", "진료과명은 필수입니다."));

                then(adminDepartmentService).should().getDepartmentDetail(5L);
                then(adminDepartmentService).should(never()).updateDepartmentName(any(), anyString());
        }

        @Test
        @DisplayName("update redirects back to detail when service validation fails")
        void update_redirectsBackToDetailWhenServiceValidationFails() throws Exception {
                // given
                given(adminDepartmentService.updateDepartmentName(5L, "Dept"))
                                .willThrow(CustomException.conflict("DUPLICATE_DEPARTMENT_NAME", "duplicate name"));

                // when
                // then
                mockMvc.perform(post("/admin/department/update")
                                .param("departmentId", "5")
                                .param("name", "Dept")
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/department/detail?departmentId=5"))
                                .andExpect(flash().attribute("errorMessage", "duplicate name"));
        }

        @Test
        @DisplayName("update redirects to list when target is missing")
        void update_redirectsToListWhenDepartmentMissing() throws Exception {
                // given
                given(adminDepartmentService.updateDepartmentName(99L, "Dept"))
                                .willThrow(CustomException.notFound("not found"));

                // when
                // then
                mockMvc.perform(post("/admin/department/update")
                                .param("departmentId", "99")
                                .param("name", "Dept")
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/department/list"))
                                .andExpect(flash().attribute("errorMessage", "not found"));
        }

        @Test
        @DisplayName("deactivate redirects to detail on success")
        void deactivate_redirectsToDetailWithSuccessMessage() throws Exception {
                // given
                given(adminDepartmentService.deactivateDepartment(5L)).willReturn("deactivated");

                // when
                // then
                mockMvc.perform(post("/admin/department/deactivate")
                                .param("departmentId", "5")
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/department/detail?departmentId=5"))
                                .andExpect(flash().attribute("successMessage", "deactivated"));

                then(adminDepartmentService).should().deactivateDepartment(5L);
        }

        @Test
        @DisplayName("deactivate redirects back to detail on duplicate request")
        void deactivate_redirectsBackToDetailWhenAlreadyInactive() throws Exception {
                // given
                given(adminDepartmentService.deactivateDepartment(5L))
                                .willThrow(CustomException.invalidStatusTransition("already inactive"));

                // when
                // then
                mockMvc.perform(post("/admin/department/deactivate")
                                .param("departmentId", "5")
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/department/detail?departmentId=5"))
                                .andExpect(flash().attribute("errorMessage", "already inactive"));
        }

        @Test
        @DisplayName("deactivate redirects to list when target is missing")
        void deactivate_redirectsToListWhenDepartmentMissing() throws Exception {
                // given
                given(adminDepartmentService.deactivateDepartment(99L))
                                .willThrow(CustomException.notFound("not found"));

                // when
                // then
                mockMvc.perform(post("/admin/department/deactivate")
                                .param("departmentId", "99")
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/department/list"))
                                .andExpect(flash().attribute("errorMessage", "not found"));
        }

        @Test
        @DisplayName("activate redirects to detail on success")
        void activate_redirectsToDetailWithSuccessMessage() throws Exception {
                // given
                given(adminDepartmentService.activateDepartment(7L)).willReturn("activated");

                // when
                // then
                mockMvc.perform(post("/admin/department/activate")
                                .param("departmentId", "7")
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/department/detail?departmentId=7"))
                                .andExpect(flash().attribute("successMessage", "activated"));

                then(adminDepartmentService).should().activateDepartment(7L);
        }

        @Test
        @DisplayName("activate redirects back to detail on duplicate request")
        void activate_redirectsBackToDetailWhenAlreadyActive() throws Exception {
                // given
                given(adminDepartmentService.activateDepartment(7L))
                                .willThrow(CustomException.invalidStatusTransition("already active"));

                // when
                // then
                mockMvc.perform(post("/admin/department/activate")
                                .param("departmentId", "7")
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/department/detail?departmentId=7"))
                                .andExpect(flash().attribute("errorMessage", "already active"));
        }

        @Test
        @DisplayName("create passes checked active flag and redirects")
        void create_passesCheckedActiveAndRedirectsToList() throws Exception {
                // given

                // when
                // then
                mockMvc.perform(post("/admin/department/create")
                                .param("name", "Dept")
                                .param("active", "true")
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/department/list"));

                then(adminDepartmentService).should().createDepartment("Dept", true);
        }

        @Test
        @DisplayName("create defaults active to false when unchecked")
        void create_defaultsActiveToFalseWhenUnchecked() throws Exception {
                // given

                // when
                // then
                mockMvc.perform(post("/admin/department/create")
                                .param("name", "Surgery")
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/department/list"));

                then(adminDepartmentService).should().createDepartment("Surgery", false);
        }

        @Test
        @DisplayName("create validation failure renders list view with error and keeps input")
        void create_validationFailure_rendersListView() throws Exception {
                // given
                AdminDepartmentListResponse response = createListResponse();
                given(adminDepartmentService.getDepartmentList(1, 10)).willReturn(response);

                // when
                // then
                mockMvc.perform(post("/admin/department/create")
                                .param("name", " ")
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/department-list"))
                .andExpect(request().attribute("model", response))
                .andExpect(request().attribute("errorMessage", SsrValidationViewSupport.INPUT_CHECK_MESSAGE))
                .andExpect(request().attribute("nameError", "진료과명은 필수입니다."))
                .andExpect(request().attribute("createName", " "))
                .andExpect(request().attribute("createActive", false))
                .andExpect(request().attribute("openCreateModal", true));

                then(adminDepartmentService).should().getDepartmentList(1, 10);
                then(adminDepartmentService).should(never()).createDepartment(anyString(), anyBoolean());
        }

        @Test
        @DisplayName("create validation failure handles empty input as field error")
        void create_validationFailure_withEmptyName_rendersListView() throws Exception {
                // given
                AdminDepartmentListResponse response = createListResponse();
                given(adminDepartmentService.getDepartmentList(1, 10)).willReturn(response);

                // when
                // then
                mockMvc.perform(post("/admin/department/create")
                                .param("name", "")
                                .param("active", "true")
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin/department-list"))
                                .andExpect(request().attribute("model", response))
                                .andExpect(request().attribute("errorMessage", SsrValidationViewSupport.INPUT_CHECK_MESSAGE))
                                .andExpect(request().attribute("nameError", "진료과명은 필수입니다."))
                                .andExpect(request().attribute("createName", ""))
                                .andExpect(request().attribute("createActive", true))
                                .andExpect(request().attribute("openCreateModal", true));

                then(adminDepartmentService).should().getDepartmentList(1, 10);
                then(adminDepartmentService).should(never()).createDepartment(anyString(), anyBoolean());
        }

        @Test
        @DisplayName("form page is not exposed")
        void formPage_isNotExposed() throws Exception {
                // given

                // when
                // then
                mockMvc.perform(get("/admin/department/form")
                                .with(user("admin").roles("ADMIN")))
                                .andExpect(status().isNotFound());
        }

        private AdminDepartmentListResponse createListResponse() {
                return new AdminDepartmentListResponse(
                                List.of(),
                                List.of(new AdminDepartmentPageLinkResponse(1, "/admin/department/list?page=1&size=10",
                                                true)),
                                0,
                                1,
                                10,
                                0,
                                false,
                                false,
                                false,
                                "",
                                "");
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
