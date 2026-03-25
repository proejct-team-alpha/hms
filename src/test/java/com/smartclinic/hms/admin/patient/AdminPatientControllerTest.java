package com.smartclinic.hms.admin.patient;

import com.smartclinic.hms.admin.patient.dto.AdminPatientDetailResponse;
import com.smartclinic.hms.admin.patient.dto.AdminPatientListResponse;
import com.smartclinic.hms.admin.patient.dto.AdminPatientPageLinkResponse;
import com.smartclinic.hms.admin.patient.dto.AdminPatientReservationHistoryItemResponse;
import com.smartclinic.hms.common.AdminControllerTestSecurityConfig;
import com.smartclinic.hms.common.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

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
    @DisplayName("목록은 기본 페이지와 pageTitle을 포함해 렌더링한다")
    void list_usesDefaultPagingAndRendersView() throws Exception {
        // given
        AdminPatientListResponse response = createEmptyListResponse();
        given(adminPatientService.getPatientList(1, 20, null)).willReturn(response);

        // when
        // then
        mockMvc.perform(get("/admin/patient/list")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/patient-list"))
                .andExpect(request().attribute("model", response))
                .andExpect(request().attribute("pageTitle", notNullValue()))
                .andExpect(content().string(containsString("환자명 또는 연락처로 환자를 빠르게 조회하고 관리합니다.")))
                .andExpect(content().string(containsString("placeholder=\"환자명 또는 연락처 검색\"")))
                .andExpect(content().string(containsString("조회")))
                .andExpect(content().string(containsString("초기화")));

        then(adminPatientService).should().getPatientList(1, 20, null);
    }

    @Test
    @DisplayName("목록은 keyword 파라미터를 서비스에 전달한다")
    void list_passesKeywordParamToService() throws Exception {
        // given
        AdminPatientListResponse response = createKeywordListResponse("kim");
        given(adminPatientService.getPatientList(2, 20, "kim")).willReturn(response);

        // when
        // then
        mockMvc.perform(get("/admin/patient/list")
                        .param("page", "2")
                        .param("size", "20")
                        .param("keyword", "kim")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/patient-list"))
                .andExpect(request().attribute("model", response));

        then(adminPatientService).should().getPatientList(2, 20, "kim");
    }

    @Test
    @DisplayName("목록은 환자 row를 렌더링한다")
    void list_rendersPatientRow() throws Exception {
        // given
        AdminPatientListResponse response = new AdminPatientListResponse(
                List.of(new AdminPatientSummary(3L, "Kim Cheolsu", "010-1234-5678", "2026-03-19", "/admin/patient/detail?patientId=3")),
                List.of(new AdminPatientPageLinkResponse(1, "/admin/patient/list?page=1&size=20", true)),
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
        given(adminPatientService.getPatientList(1, 20, null)).willReturn(response);

        // when
        // then
        mockMvc.perform(get("/admin/patient/list")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Kim Cheolsu")))
                .andExpect(content().string(containsString("010-1234-5678")))
                .andExpect(content().string(containsString("2026-03-19")))
                .andExpect(content().string(containsString("상세보기")));
    }

    @Test
    @DisplayName("목록은 검색어를 입력창 값으로 유지한다")
    void list_keepsKeywordInSearchInput() throws Exception {
        // given
        AdminPatientListResponse response = createKeywordListResponse("0101234");
        given(adminPatientService.getPatientList(1, 20, "0101234")).willReturn(response);

        // when
        // then
        mockMvc.perform(get("/admin/patient/list")
                        .param("keyword", "0101234")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"0101234\"")));
    }

    @Test
    @DisplayName("목록은 빈 결과 상태를 렌더링한다")
    void list_rendersEmptyStateWhenNoPatientsFound() throws Exception {
        // given
        AdminPatientListResponse response = createEmptyListResponse();
        given(adminPatientService.getPatientList(1, 20, null)).willReturn(response);

        // when
        // then
        mockMvc.perform(get("/admin/patient/list")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/patient-list"))
                .andExpect(request().attribute("model", response))
                .andExpect(content().string(containsString("조회된 환자가 없습니다.")));
    }

    @Test
    @DisplayName("상세는 환자 정보와 예약 이력을 렌더링한다")
    void detail_rendersPatientInfoAndReservationHistories() throws Exception {
        // given
        AdminPatientDetailResponse response = new AdminPatientDetailResponse(
                7L,
                "Kim Cheolsu",
                "010-1234-5678",
                "kim@example.com",
                "Seoul Gangnam-gu",
                "Peanut allergy",
                "Peanut allergy",
                List.of(new AdminPatientReservationHistoryItemResponse(
                        "R-1001",
                        "2026-03-19",
                        "09:30",
                        "Dental",
                        "Dr. Kim",
                        "Received"
                ))
        );
        given(adminPatientService.getPatientDetail(7L)).willReturn(response);

        // when
        // then
        mockMvc.perform(get("/admin/patient/detail")
                        .param("patientId", "7")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/patient-detail"))
                .andExpect(request().attribute("model", response))
                .andExpect(request().attribute("pageTitle", notNullValue()))
                .andExpect(content().string(containsString("Kim Cheolsu")))
                .andExpect(content().string(containsString("010-1234-5678")))
                .andExpect(content().string(containsString("kim@example.com")))
                .andExpect(content().string(containsString("R-1001")))
                .andExpect(content().string(containsString("Received")))
                .andExpect(content().string(containsString("data-patient-edit-toggle")))
                .andExpect(content().string(containsString("data-patient-edit-form")))
                .andExpect(content().string(containsString("/admin/api/patients/7/update")));

        then(adminPatientService).should().getPatientDetail(7L);
    }

    @Test
    @DisplayName("상세는 없는 환자 요청 시 404를 반환한다")
    void detail_returns404WhenPatientIsMissing() throws Exception {
        // given
        given(adminPatientService.getPatientDetail(99L))
                .willThrow(CustomException.notFound("patient not found"));

        // when
        // then
        mockMvc.perform(get("/admin/patient/detail")
                        .param("patientId", "99")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(request().attribute("errorMessage", "patient not found"));
    }

    private AdminPatientListResponse createEmptyListResponse() {
        return new AdminPatientListResponse(
                List.of(),
                List.of(),
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

    private AdminPatientListResponse createKeywordListResponse(String keyword) {
        return new AdminPatientListResponse(
                List.of(),
                List.of(new AdminPatientPageLinkResponse(1, "/admin/patient/list?page=1&size=20&keyword=" + keyword, true)),
                keyword,
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