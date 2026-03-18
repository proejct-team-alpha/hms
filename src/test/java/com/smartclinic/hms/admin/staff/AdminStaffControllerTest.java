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
import com.smartclinic.hms.common.AdminControllerTestSecurityConfig;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
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
@Import(AdminControllerTestSecurityConfig.class)
class AdminStaffControllerTest {

    private static final String STAFF_CREATED_MESSAGE = "직원을 등록했습니다.";
    private static final String STAFF_UPDATED_MESSAGE = "직원 정보를 수정했습니다.";
    private static final String STAFF_DEACTIVATED_MESSAGE = "직원을 비활성화했습니다.";
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
    }

    @Test
    @DisplayName("직원 목록 뷰는 model과 pageTitle을 함께 렌더링한다")
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
    @DisplayName("직원 등록 성공 시 목록으로 리다이렉트한다")
    void create_success_redirectsToList() throws Exception {
        // given
        given(adminStaffService.createStaff(any())).willReturn(STAFF_CREATED_MESSAGE);

        // when
        // then
        mockMvc.perform(post("/admin/staff/create")
                        .param("username", "doctor-new")
                        .param("password", "password123")
                        .param("name", "신규의사")
                        .param("employeeNumber", "D-NEW-001")
                        .param("role", "DOCTOR")
                        .param("departmentId", "1")
                        .param("active", "true")
                        .param("specialty", "가정의학")
                        .param("availableDays", "MON")
                        .param("availableDays", "WED")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("/admin/staff/list")))
                .andExpect(flash().attribute("successMessage", STAFF_CREATED_MESSAGE));
    }

    @Test
    @DisplayName("직원 등록 검증 실패 시 등록 폼을 다시 렌더링한다")
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
                        .param("specialty", "가정의학")
                        .param("availableDays", "MON")
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
    @DisplayName("직원 비활성화 성공 시 목록으로 리다이렉트한다")
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
                        "김직원",
                        "staff01",
                        "S-001",
                        "STAFF",
                        "직원",
                        "bg-purple-100 text-purple-800",
                        "내과",
                        true,
                        "재직",
                        "bg-green-100 text-green-800",
                        "/admin/staff/detail?staffId=2",
                        true,
                        ""
                )),
                List.of(new AdminStaffFilterOptionResponse("ALL", "전체 역할", "ALL".equals(selectedRole))),
                List.of(new AdminStaffFilterOptionResponse("ALL", "전체 상태", "ALL".equals(selectedEmploymentStatus))),
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
                "직원 등록",
                "/admin/staff/create",
                "등록하기",
                false,
                null,
                "",
                "",
                "",
                "STAFF",
                "직원",
                null,
                true,
                false,
                "",
                List.of(new AdminStaffFormOptionResponse("STAFF", "직원", true)),
                List.of(new AdminStaffDepartmentOptionResponse(1L, "내과", false)),
                List.of(new AdminStaffFormOptionResponse("true", "재직", true)),
                List.of()
        );
    }

    private AdminStaffFormResponse createDoctorCreateFormResponse() {
        return new AdminStaffFormResponse(
                "직원 등록",
                "/admin/staff/create",
                "등록하기",
                false,
                null,
                "",
                "",
                "",
                "DOCTOR",
                "의사",
                null,
                true,
                true,
                "가정의학",
                List.of(new AdminStaffFormOptionResponse("DOCTOR", "의사", true)),
                List.of(new AdminStaffDepartmentOptionResponse(1L, "가정의학과", false)),
                List.of(new AdminStaffFormOptionResponse("true", "재직", true)),
                List.of(new AdminStaffFormOptionResponse("MON", "월요일", true))
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
}