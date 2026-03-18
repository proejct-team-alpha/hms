package com.smartclinic.hms.admin.department;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.smartclinic.hms.common.exception.CustomException;
import com.smartclinic.hms.domain.Department;
import java.util.List;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = AdminDepartmentController.class,
        properties = {
                "spring.mustache.servlet.expose-request-attributes=true",
                "spring.mustache.servlet.allow-request-override=true"
        }
)
@Import(AdminDepartmentControllerTest.TestSecurityConfig.class)
class AdminDepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminDepartmentService adminDepartmentService;

    @Test
    @DisplayName("진료과 목록은 기본 페이지 파라미터와 model, pageTitle로 렌더링한다")
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
                .andExpect(request().attribute("model", response))
                .andExpect(request().attribute("pageTitle", "진료과 관리"));

        then(adminDepartmentService).should().getDepartmentList(1, 10);
    }

    @Test
    @DisplayName("진료과 목록은 요청 page, size 파라미터를 서비스에 전달한다")
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
                .andExpect(request().attribute("model", response))
                .andExpect(request().attribute("pageTitle", "진료과 관리"));

        then(adminDepartmentService).should().getDepartmentList(2, 5);
    }

    @Test
    @DisplayName("진료과 목록은 페이지 정보와 번호 링크를 화면에 렌더링한다")
    void list_rendersPaginationUi() throws Exception {
        // given
        Department department = Department.create("내과", true);
        ReflectionTestUtils.setField(department, "id", 12L);
        AdminDepartmentDto item = new AdminDepartmentDto(department);
        AdminDepartmentListResponse response = new AdminDepartmentListResponse(
                List.of(item),
                List.of(
                        new AdminDepartmentPageLinkResponse(1, "/admin/department/list?page=1&size=5", false),
                        new AdminDepartmentPageLinkResponse(2, "/admin/department/list?page=2&size=5", true),
                        new AdminDepartmentPageLinkResponse(3, "/admin/department/list?page=3&size=5", false)
                ),
                12,
                2,
                5,
                3,
                true,
                true,
                true,
                "/admin/department/list?page=1&size=5",
                "/admin/department/list?page=3&size=5"
        );
        given(adminDepartmentService.getDepartmentList(2, 5)).willReturn(response);

        // when
        // then
        mockMvc.perform(get("/admin/department/list")
                        .param("page", "2")
                        .param("size", "5")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("내과")))
                .andExpect(content().string(containsString("총")))
                .andExpect(content().string(containsString("2 / 3페이지")))
                .andExpect(content().string(containsString("/admin/department/detail?departmentId=12")))
                .andExpect(content().string(containsString(">1</a>")))
                .andExpect(content().string(containsString(">3</a>")));
    }

    @Test
    @DisplayName("진료과 목록은 빈 데이터일 때 빈 목록 메시지와 0페이지 정보를 렌더링한다")
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
                .andExpect(content().string(containsString("등록된 진료과가 없습니다.")))
                .andExpect(content().string(containsString("0 / 0페이지")));
    }

    @Test
    @DisplayName("진료과 상세 화면은 기본 정보와 상태 액션을 렌더링한다")
    void detail_rendersDepartmentDetailView() throws Exception {
        // given
        AdminDepartmentDetailResponse response = new AdminDepartmentDetailResponse(
                3L,
                "내과",
                true,
                "운영 중",
                "px-2 py-1 text-xs font-medium rounded-full bg-green-100 text-green-700",
                false,
                true,
                "/admin/department/update",
                "/admin/department/activate",
                "/admin/department/deactivate"
        );
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
                .andExpect(request().attribute("pageTitle", "진료과 상세"))
                .andExpect(content().string(containsString("내과")))
                .andExpect(content().string(containsString("이름 수정")))
                .andExpect(content().string(containsString("비활성화")));
    }

    @Test
    @DisplayName("존재하지 않는 진료과 상세 요청은 404 화면을 반환한다")
    void detail_returns404WhenDepartmentMissing() throws Exception {
        // given
        given(adminDepartmentService.getDepartmentDetail(99L))
                .willThrow(CustomException.notFound("진료과를 찾을 수 없습니다."));

        // when
        // then
        mockMvc.perform(get("/admin/department/detail")
                        .param("departmentId", "99")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(request().attribute("errorMessage", "진료과를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("진료과 등록은 active 체크 여부를 서비스에 전달하고 목록으로 리다이렉트한다")
    void create_passesCheckedActiveAndRedirectsToList() throws Exception {
        // given

        // when
        // then
        mockMvc.perform(post("/admin/department/form")
                        .param("name", "내과")
                        .param("active", "true")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/department/list"));

        then(adminDepartmentService).should().createDepartment("내과", true);
    }

    @Test
    @DisplayName("진료과 등록은 active 값이 없으면 false로 저장하고 목록으로 리다이렉트한다")
    void create_defaultsActiveToFalseWhenUnchecked() throws Exception {
        // given

        // when
        // then
        mockMvc.perform(post("/admin/department/form")
                        .param("name", "외과")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/department/list"));

        then(adminDepartmentService).should().createDepartment("외과", false);
    }

    private AdminDepartmentListResponse createListResponse() {
        return new AdminDepartmentListResponse(
                List.of(),
                List.of(new AdminDepartmentPageLinkResponse(1, "/admin/department/list?page=1&size=10", true)),
                0,
                1,
                10,
                0,
                false,
                false,
                false,
                "",
                ""
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