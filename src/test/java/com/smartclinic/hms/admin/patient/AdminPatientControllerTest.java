package com.smartclinic.hms.admin.patient;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.smartclinic.hms.admin.patient.dto.AdminPatientListResponse;
import com.smartclinic.hms.admin.patient.dto.AdminPatientPageLinkResponse;
import com.smartclinic.hms.common.AdminControllerTestSecurityConfig;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = AdminPatientController.class,
        properties = {
                "spring.mustache.servlet.expose-request-attributes=true",
                "spring.mustache.servlet.allow-request-override=true"
        }
)
@Import(AdminControllerTestSecurityConfig.class)
class AdminPatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminPatientService adminPatientService;

    @Test
    @DisplayName("list uses default paging and renders view")
    void list_usesDefaultPagingAndRendersView() throws Exception {
        // given
        AdminPatientListResponse response = createListResponse();
        given(adminPatientService.getPatientList(1, 20, null, null)).willReturn(response);

        // when
        // then
        mockMvc.perform(get("/admin/patient/list")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/patient-list"))
                .andExpect(request().attribute("model", response))
                .andExpect(request().attribute("pageTitle", "환자 관리"));

        then(adminPatientService).should().getPatientList(1, 20, null, null);
    }

    @Test
    @DisplayName("list passes request params to service")
    void list_passesRequestParamsToService() throws Exception {
        // given
        AdminPatientListResponse response = createListResponse();
        given(adminPatientService.getPatientList(2, 20, "kim", "0101234")).willReturn(response);

        // when
        // then
        mockMvc.perform(get("/admin/patient/list")
                        .param("page", "2")
                        .param("size", "20")
                        .param("nameKeyword", "kim")
                        .param("contactKeyword", "0101234")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/patient-list"))
                .andExpect(request().attribute("model", response));

        then(adminPatientService).should().getPatientList(2, 20, "kim", "0101234");
    }

    @Test
    @DisplayName("list renders patient row and empty state copy")
    void list_rendersPatientRowAndEmptyStateCopy() throws Exception {
        // given
        AdminPatientListResponse response = new AdminPatientListResponse(
                List.of(new AdminPatientSummary(3L, "김철수", "010-1234-5678", "2026-03-19", "/admin/patient/detail?patientId=3")),
                List.of(new AdminPatientPageLinkResponse(1, "/admin/patient/list?page=1&size=20", true)),
                "",
                "",
                1,
                1,
                20,
                1,
                true,
                false,
                false,
                "",
                ""
        );
        given(adminPatientService.getPatientList(1, 20, null, null)).willReturn(response);

        // when
        // then
        mockMvc.perform(get("/admin/patient/list")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("이름 또는 연락처로 환자를 검색하세요.")))
                .andExpect(content().string(containsString("김철수")))
                .andExpect(content().string(containsString("010-1234-5678")))
                .andExpect(content().string(containsString("2026-03-19")));
    }

    private AdminPatientListResponse createListResponse() {
        return new AdminPatientListResponse(
                List.of(),
                List.of(),
                "",
                "",
                0,
                1,
                20,
                0,
                false,
                false,
                false,
                "",
                ""
        );
    }
}
