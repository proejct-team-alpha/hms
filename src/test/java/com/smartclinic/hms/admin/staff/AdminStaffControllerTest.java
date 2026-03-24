package com.smartclinic.hms.admin.staff;

import com.smartclinic.hms.admin.staff.dto.AdminStaffDepartmentOptionResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffFilterOptionResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffFormOptionResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffFormResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffItemResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffListResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffPageLinkResponse;
import com.smartclinic.hms.admin.staff.dto.UpdateAdminStaffRequest;
import com.smartclinic.hms.common.AdminControllerTestSecurityConfig;
import com.smartclinic.hms.common.exception.CustomException;
import com.smartclinic.hms.common.util.SsrValidationViewSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

    private static final String STAFF_CREATED_MESSAGE = "staff created";
    private static final String STAFF_UPDATED_MESSAGE = "staff updated";
    private static final String STAFF_DEACTIVATED_MESSAGE = "staff deactivated";
    private static final String INVALID_DEPARTMENT_MESSAGE = "department is required";
    private static final String SELF_DEACTIVATE_MESSAGE = "self deactivate is not allowed";
    private static final String INACTIVE_UPDATE_NOT_ALLOWED_MESSAGE = "\uBE44\uD65C\uC131\uD654\uB41C \uC9C1\uC6D0\uC740 \uC218\uC815\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4.";
    private static final String READ_ONLY_MESSAGE = "\uBE44\uD65C\uC131\uD654\uB41C \uC9C1\uC6D0\uC785\uB2C8\uB2E4. \uC870\uD68C\uB9CC \uAC00\uB2A5\uD569\uB2C8\uB2E4.";
    private static final String RETIRED_AT_REQUIRED_MESSAGE = "\uD1F4\uC0AC \uC77C\uC2DC\uB294 \uB0A0\uC9DC\uC640 \uC2DC\uAC04\uC744 \uBAA8\uB450 \uC120\uD0DD\uD574\uC57C \uD569\uB2C8\uB2E4.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminStaffService adminStaffService;

    @Test
    @DisplayName("new form renders staff form view")
    void newForm_rendersStaffFormView() throws Exception {
        AdminStaffFormResponse response = createFormResponse();
        given(adminStaffService.getCreateForm()).willReturn(response);

        mockMvc.perform(get("/admin/staff/new")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/staff-form"))
                .andExpect(request().attribute("model", response));

        then(adminStaffService).should().getCreateForm();
    }

    @Test
    @DisplayName("new doctor form renders retiredAt date and hour inputs")
    void newForm_doctorCreateForm_rendersDepartmentAndRetiredAtFields() throws Exception {
        AdminStaffFormResponse response = createDoctorCreateFormResponse(
                "doctor-new",
                "doctor name",
                "D-NEW-001",
                1L,
                true,
                "2026-03-31T18:00");
        given(adminStaffService.getCreateForm()).willReturn(response);

        mockMvc.perform(get("/admin/staff/new")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/staff-form"))
                .andExpect(content().string(containsString("name=\"departmentId\"")))
                .andExpect(content().string(containsString("id=\"staff-retired-at-date\"")))
                .andExpect(content().string(containsString("name=\"retiredAtDate\"")))
                .andExpect(content().string(containsString("value=\"2026-03-31\"")))
                .andExpect(content().string(containsString("id=\"staff-retired-at-hour\"")))
                .andExpect(content().string(containsString("<option value=\"18\" selected>18:00</option>")))
                .andExpect(content().string(containsString("id=\"staff-retired-at-hidden\"")))
                .andExpect(content().string(containsString("name=\"retiredAt\" value=\"2026-03-31T18:00\"")))
                .andExpect(content().string(not(containsString("id=\"staff-retired-at\""))));
    }

    @Test
    @DisplayName("detail renders edit form view")
    void detail_rendersEditFormView() throws Exception {
        AdminStaffFormResponse response = createEditFormResponse(false, true, false, "");
        given(adminStaffService.getEditForm(1L, "admin")).willReturn(response);

        mockMvc.perform(get("/admin/staff/detail")
                        .param("staffId", "1")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/staff-form"))
                .andExpect(request().attribute("model", response));
    }

    @Test
    @DisplayName("edit form renders retiredAt date and hour inputs")
    void detail_editMode_rendersRetiredAtDateAndHourInputs() throws Exception {
        AdminStaffFormResponse response = createEditFormResponse(false, true, false, "2026-03-31T18:00");
        given(adminStaffService.getEditForm(1L, "admin")).willReturn(response);

        mockMvc.perform(get("/admin/staff/detail")
                        .param("staffId", "1")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"staff-retired-at-date\"")))
                .andExpect(content().string(containsString("name=\"retiredAtDate\"")))
                .andExpect(content().string(containsString("value=\"2026-03-31\"")))
                .andExpect(content().string(containsString("id=\"staff-retired-at-hour\"")))
                .andExpect(content().string(containsString("<option value=\"18\" selected>18:00</option>")))
                .andExpect(content().string(containsString("id=\"staff-retired-at-hidden\"")))
                .andExpect(content().string(containsString("name=\"retiredAt\" value=\"2026-03-31T18:00\"")));
    }

    @Test
    @DisplayName("inactive detail renders read only form")
    void detail_inactiveStaff_rendersReadOnlyForm() throws Exception {
        AdminStaffFormResponse response = createEditFormResponse(false, false, false, "2026-03-31T18:00");
        given(adminStaffService.getEditForm(1L, "admin")).willReturn(response);

        mockMvc.perform(get("/admin/staff/detail")
                        .param("staffId", "1")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(READ_ONLY_MESSAGE)))
                .andExpect(content().string(not(containsString("data-feather=\"save\""))))
                .andExpect(content().string(containsString("disabled")));
    }

    @Test
    @DisplayName("self edit keeps active and retiredAt hidden values")
    void detail_selfEdit_locksEmploymentStatusAndRetiredAt() throws Exception {
        AdminStaffFormResponse response = createEditFormResponse(true, true, true, "2026-03-31T18:00");
        given(adminStaffService.getEditForm(1L, "doctor01")).willReturn(response);

        mockMvc.perform(get("/admin/staff/detail")
                        .param("staffId", "1")
                        .with(user("doctor01").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"active\" value=\"true\"")))
                .andExpect(content().string(containsString("name=\"retiredAt\" value=\"2026-03-31T18:00\"")));
    }

    @Test
    @DisplayName("list uses default paging and renders view")
    void list_usesDefaultPagingAndRendersView() throws Exception {
        AdminStaffListResponse response = createListResponse("ALL", "ALL", "");
        given(adminStaffService.getStaffList(1, 10, null, null, null, "admin")).willReturn(response);

        mockMvc.perform(get("/admin/staff/list")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/staff-list"))
                .andExpect(request().attribute("model", response))
                .andExpect(request().attribute("pageTitle", notNullValue()));
    }

    @Test
    @DisplayName("create success redirects to list")
    void create_success_redirectsToList() throws Exception {
        given(adminStaffService.createStaff(any())).willReturn(STAFF_CREATED_MESSAGE);

        mockMvc.perform(post("/admin/staff/create")
                        .param("username", "doctor-new")
                        .param("password", "password123")
                        .param("name", "doctor name")
                        .param("employeeNumber", "D-NEW-001")
                        .param("role", "DOCTOR")
                        .param("departmentId", "1")
                        .param("active", "true")
                        .param("retiredAt", "2026-03-31T18:00")
                        .param("availableDays", "MON")
                        .param("availableDays", "WED")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("/admin/staff/list")))
                .andExpect(flash().attribute("successMessage", STAFF_CREATED_MESSAGE));
    }

    @Test
    @DisplayName("create validation failure without doctor department renders form")
    void create_validationFailure_withoutDoctorDepartment_rendersStaffFormView() throws Exception {
        AdminStaffFormResponse response = createDoctorCreateFormResponse(
                "doctor-new",
                "doctor name",
                "D-NEW-001",
                null,
                true,
                "2026-03-31T18:00");
        given(adminStaffService.createStaff(any()))
                .willThrow(CustomException.badRequest("VALIDATION_ERROR", INVALID_DEPARTMENT_MESSAGE));
        given(adminStaffService.getCreateForm(any())).willReturn(response);

        mockMvc.perform(post("/admin/staff/create")
                        .param("username", "doctor-new")
                        .param("password", "password123")
                        .param("name", "doctor name")
                        .param("employeeNumber", "D-NEW-001")
                        .param("role", "DOCTOR")
                        .param("active", "true")
                        .param("retiredAt", "2026-03-31T18:00")
                        .param("availableDays", "MON")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/staff-form"))
                .andExpect(request().attribute("errorMessage", INVALID_DEPARTMENT_MESSAGE))
                .andExpect(request().attribute("model", response));

        then(adminStaffService).should().createStaff(any());
        then(adminStaffService).should().getCreateForm(any());
    }

    @Test
    @DisplayName("update success redirects to list")
    void update_success_redirectsToList() throws Exception {
        given(adminStaffService.updateStaff(any(UpdateAdminStaffRequest.class), eq("admin")))
                .willReturn(STAFF_UPDATED_MESSAGE);

        mockMvc.perform(post("/admin/staff/update")
                        .param("staffId", "1")
                        .param("name", "updated staff")
                        .param("departmentId", "2")
                        .param("password", "")
                        .param("active", "true")
                        .param("retiredAt", "2026-03-31T18:00")
                        .param("availableDays", "MON")
                        .param("availableDays", "WED")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("/admin/staff/list")))
                .andExpect(flash().attribute("successMessage", STAFF_UPDATED_MESSAGE));
    }

    @Test
    @DisplayName("update validation failure renders edit form")
    void update_validationFailure_rendersStaffFormView() throws Exception {
        AdminStaffFormResponse response = createEditFormResponse(false, true, false, "2026-03-31T18:00");
        given(adminStaffService.updateStaff(any(UpdateAdminStaffRequest.class), eq("admin")))
                .willThrow(CustomException.badRequest("VALIDATION_ERROR", INVALID_DEPARTMENT_MESSAGE));
        given(adminStaffService.getEditForm(any(UpdateAdminStaffRequest.class), eq("admin"))).willReturn(response);

        mockMvc.perform(post("/admin/staff/update")
                        .param("staffId", "1")
                        .param("name", "updated staff")
                        .param("password", "")
                        .param("active", "true")
                        .param("retiredAt", "2026-03-31T18:00")
                        .param("availableDays", "MON")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/staff-form"))
                .andExpect(request().attribute("errorMessage", INVALID_DEPARTMENT_MESSAGE))
                .andExpect(request().attribute("model", response));

        then(adminStaffService).should().updateStaff(any(UpdateAdminStaffRequest.class), eq("admin"));
        then(adminStaffService).should().getEditForm(any(UpdateAdminStaffRequest.class), eq("admin"));
    }

    @Test
    @DisplayName("self deactivate attempt renders edit form")
    void update_selfDeactivateAttempt_rendersStaffFormView() throws Exception {
        AdminStaffFormResponse response = createEditFormResponse(true, true, true, "2026-03-31T18:00");
        given(adminStaffService.updateStaff(any(UpdateAdminStaffRequest.class), eq("doctor01")))
                .willThrow(CustomException.badRequest("VALIDATION_ERROR", SELF_DEACTIVATE_MESSAGE));
        given(adminStaffService.getEditForm(any(UpdateAdminStaffRequest.class), eq("doctor01"))).willReturn(response);

        mockMvc.perform(post("/admin/staff/update")
                        .param("staffId", "1")
                        .param("name", "updated staff")
                        .param("departmentId", "1")
                        .param("password", "")
                        .param("active", "false")
                        .param("retiredAt", "2026-03-31T18:00")
                        .param("availableDays", "MON")
                        .with(user("doctor01").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/staff-form"))
                .andExpect(request().attribute("errorMessage", SELF_DEACTIVATE_MESSAGE))
                .andExpect(request().attribute("model", response));

        then(adminStaffService).should().updateStaff(any(UpdateAdminStaffRequest.class), eq("doctor01"));
        then(adminStaffService).should().getEditForm(any(UpdateAdminStaffRequest.class), eq("doctor01"));
    }

    @Test
    @DisplayName("inactive staff update attempt renders read only form")
    void update_inactiveStaffAttempt_rendersStaffFormView() throws Exception {
        AdminStaffFormResponse response = createEditFormResponse(false, false, false, "");
        given(adminStaffService.updateStaff(any(UpdateAdminStaffRequest.class), eq("admin")))
                .willThrow(CustomException.badRequest("VALIDATION_ERROR", INACTIVE_UPDATE_NOT_ALLOWED_MESSAGE));
        given(adminStaffService.getEditForm(any(UpdateAdminStaffRequest.class), eq("admin"))).willReturn(response);

        mockMvc.perform(post("/admin/staff/update")
                        .param("staffId", "1")
                        .param("name", "inactive staff")
                        .param("departmentId", "1")
                        .param("password", "")
                        .param("active", "true")
                        .param("retiredAt", "")
                        .param("availableDays", "MON")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/staff-form"))
                .andExpect(request().attribute("errorMessage", INACTIVE_UPDATE_NOT_ALLOWED_MESSAGE))
                .andExpect(request().attribute("model", response));

        then(adminStaffService).should().updateStaff(any(UpdateAdminStaffRequest.class), eq("admin"));
        then(adminStaffService).should().getEditForm(any(UpdateAdminStaffRequest.class), eq("admin"));
    }

    @Test
    @DisplayName("update retiredAt pair validation failure renders retiredAt error")
    void update_retiredAtPairValidationFailure_rendersRetiredAtError() throws Exception {
        AdminStaffFormResponse response = createEditFormResponse(false, true, false, "");
        given(adminStaffService.updateStaff(any(UpdateAdminStaffRequest.class), eq("admin")))
                .willThrow(CustomException.badRequest("VALIDATION_ERROR", RETIRED_AT_REQUIRED_MESSAGE));
        given(adminStaffService.getEditForm(any(UpdateAdminStaffRequest.class), eq("admin"))).willReturn(response);

        mockMvc.perform(post("/admin/staff/update")
                        .param("staffId", "1")
                        .param("name", "updated staff")
                        .param("departmentId", "1")
                        .param("password", "")
                        .param("active", "true")
                        .param("retiredAtDate", "2026-03-31")
                        .param("retiredAtHour", "")
                        .param("retiredAt", "")
                        .param("availableDays", "MON")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/staff-form"))
                .andExpect(request().attribute("errorMessage", RETIRED_AT_REQUIRED_MESSAGE))
                .andExpect(request().attribute("retiredAtError", RETIRED_AT_REQUIRED_MESSAGE))
                .andExpect(request().attribute("model", response));

        then(adminStaffService).should().updateStaff(any(UpdateAdminStaffRequest.class), eq("admin"));
        then(adminStaffService).should().getEditForm(any(UpdateAdminStaffRequest.class), eq("admin"));
    }

    @Test
    @DisplayName("binding failure with empty name renders edit form")
    void update_bindingFailure_withEmptyName_rendersStaffFormView() throws Exception {
        AdminStaffFormResponse response = createEditFormResponse(false, true, false, "");
        given(adminStaffService.getEditForm(any(UpdateAdminStaffRequest.class), eq("admin"))).willReturn(response);

        mockMvc.perform(post("/admin/staff/update")
                        .param("staffId", "1")
                        .param("name", "")
                        .param("departmentId", "1")
                        .param("password", "")
                        .param("active", "true")
                        .param("retiredAt", "")
                        .param("availableDays", "MON")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/staff-form"))
                .andExpect(request().attribute("errorMessage", SsrValidationViewSupport.INPUT_CHECK_MESSAGE))
                .andExpect(request().attribute("nameError", notNullValue()))
                .andExpect(request().attribute("model", response));

        then(adminStaffService).should().getEditForm(any(UpdateAdminStaffRequest.class), eq("admin"));
        then(adminStaffService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("deactivate success redirects to list")
    void deactivate_success_redirectsToList() throws Exception {
        given(adminStaffService.deactivateStaff(2L, "admin")).willReturn(STAFF_DEACTIVATED_MESSAGE);

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
                        "staff name",
                        "staff01",
                        "S-001",
                        "STAFF",
                        "staff",
                        "bg-purple-100 text-purple-800",
                        "-",
                        true,
                        "active",
                        "bg-green-100 text-green-800",
                        "/admin/staff/detail?staffId=2",
                        true,
                        ""
                )),
                List.of(new AdminStaffFilterOptionResponse("ALL", "all roles", "ALL".equals(selectedRole))),
                List.of(new AdminStaffFilterOptionResponse("ALL", "all statuses", "ALL".equals(selectedEmploymentStatus))),
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
                "staff create",
                "/admin/staff/create",
                "submit",
                false,
                null,
                "",
                "",
                "",
                "STAFF",
                "staff",
                null,
                true,
                "",
                "",
                "",
                false,
                false,
                false,
                false,
                false,
                List.of(new AdminStaffFormOptionResponse("STAFF", "staff", true)),
                List.of(new AdminStaffDepartmentOptionResponse(1L, "dept", false)),
                List.of(new AdminStaffFormOptionResponse("true", "active", true)),
                List.of(),
                List.of()
        );
    }

    private AdminStaffFormResponse createDoctorCreateFormResponse(
            String username,
            String name,
            String employeeNumber,
            Long selectedDepartmentId,
            boolean active,
            String retiredAt
    ) {
        return new AdminStaffFormResponse(
                "staff create",
                "/admin/staff/create",
                "submit",
                false,
                null,
                username,
                name,
                employeeNumber,
                "DOCTOR",
                "doctor",
                selectedDepartmentId,
                active,
                retiredAt,
                extractRetiredAtDate(retiredAt),
                extractRetiredAtHour(retiredAt),
                false,
                false,
                false,
                false,
                true,
                List.of(new AdminStaffFormOptionResponse("DOCTOR", "doctor", true)),
                List.of(new AdminStaffDepartmentOptionResponse(1L, "family", selectedDepartmentId != null && selectedDepartmentId.equals(1L))),
                List.of(
                        new AdminStaffFormOptionResponse("true", "active", active),
                        new AdminStaffFormOptionResponse("false", "inactive", !active)
                ),
                buildRetiredAtHourOptions(extractRetiredAtHour(retiredAt)),
                List.of(new AdminStaffFormOptionResponse("MON", "mon", true))
        );
    }

    private AdminStaffFormResponse createEditFormResponse(
            boolean selfEdit,
            boolean active,
            boolean retiredAtLocked,
            String retiredAt
    ) {
        boolean readOnly = !active;
        boolean employmentStatusLocked = selfEdit || readOnly;
        boolean effectiveRetiredAtLocked = retiredAtLocked || readOnly;
        return new AdminStaffFormResponse(
                "staff edit",
                "/admin/staff/update",
                "submit",
                true,
                1L,
                "doctor01",
                "doctor name",
                "D-001",
                "DOCTOR",
                "doctor",
                1L,
                active,
                retiredAt,
                extractRetiredAtDate(retiredAt),
                extractRetiredAtHour(retiredAt),
                selfEdit,
                employmentStatusLocked,
                effectiveRetiredAtLocked,
                readOnly,
                true,
                List.of(new AdminStaffFormOptionResponse("DOCTOR", "doctor", true)),
                List.of(new AdminStaffDepartmentOptionResponse(1L, "dept", true)),
                List.of(
                        new AdminStaffFormOptionResponse("true", "active", active),
                        new AdminStaffFormOptionResponse("false", "inactive", !active)
                ),
                buildRetiredAtHourOptions(extractRetiredAtHour(retiredAt)),
                List.of(new AdminStaffFormOptionResponse("MON", "mon", true))
        );
    }

    private String extractRetiredAtDate(String retiredAt) {
        if (retiredAt == null || retiredAt.isBlank()) {
            return "";
        }
        return retiredAt.substring(0, 10);
    }

    private String extractRetiredAtHour(String retiredAt) {
        if (retiredAt == null || retiredAt.isBlank()) {
            return "";
        }
        return retiredAt.substring(11, 13);
    }

    private List<AdminStaffFormOptionResponse> buildRetiredAtHourOptions(String selectedHour) {
        if (selectedHour == null || selectedHour.isBlank()) {
            return List.of();
        }
        return List.of(new AdminStaffFormOptionResponse(selectedHour, selectedHour + ":00", true));
    }
}
