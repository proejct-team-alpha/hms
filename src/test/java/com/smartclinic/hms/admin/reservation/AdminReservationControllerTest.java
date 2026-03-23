package com.smartclinic.hms.admin.reservation;

import com.smartclinic.hms.admin.reservation.dto.AdminReservationItemResponse;
import com.smartclinic.hms.admin.reservation.dto.AdminReservationListResponse;
import com.smartclinic.hms.admin.reservation.dto.AdminReservationPageLinkResponse;
import com.smartclinic.hms.admin.reservation.dto.AdminReservationStatusOptionResponse;
import com.smartclinic.hms.common.AdminControllerTestSecurityConfig;
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
        value = AdminReservationController.class,
        properties = {
                "spring.mustache.servlet.expose-request-attributes=true",
                "spring.mustache.servlet.allow-request-override=true"
        }
)
@Import(AdminControllerTestSecurityConfig.class)
class AdminReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminReservationService adminReservationService;

    @Test
    @DisplayName("예약 목록은 기본 페이지 정보와 화면 타이틀을 렌더링한다")
    void list_usesDefaultPagingAndRendersView() throws Exception {
        AdminReservationListResponse viewModel = emptyResponse("ALL", "");
        given(adminReservationService.getReservationList(1, 10, null, null)).willReturn(viewModel);

        mockMvc.perform(get("/admin/reservation/list")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reservation-list"))
                .andExpect(request().attribute("model", viewModel))
                .andExpect(request().attribute("pageTitle", "예약 목록"))
                .andExpect(content().string(containsString("환자명 또는 연락처로 예약과 접수 현황을 빠르게 조회합니다.")))
                .andExpect(content().string(containsString("placeholder=\"환자명 또는 연락처 검색\"")))
                .andExpect(content().string(containsString("조회")))
                .andExpect(content().string(containsString("초기화")));

        then(adminReservationService).should().getReservationList(1, 10, null, null);
    }

    @Test
    @DisplayName("예약 목록은 요청 파라미터를 서비스에 그대로 전달한다")
    void list_passesRequestParamsToService() throws Exception {
        AdminReservationListResponse viewModel = new AdminReservationListResponse(
                List.of(),
                List.of(),
                List.of(),
                "RESERVED",
                "김",
                0,
                2,
                5,
                0,
                true,
                false,
                "/admin/reservation/list?page=1&size=5&status=RESERVED&keyword=김",
                ""
        );
        given(adminReservationService.getReservationList(2, 5, "RESERVED", "김")).willReturn(viewModel);

        mockMvc.perform(get("/admin/reservation/list")
                        .param("page", "2")
                        .param("size", "5")
                        .param("status", "RESERVED")
                        .param("keyword", "김")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reservation-list"))
                .andExpect(request().attribute("model", viewModel));

        then(adminReservationService).should().getReservationList(2, 5, "RESERVED", "김");
    }

    @Test
    @DisplayName("예약 목록은 접수 상태 옵션을 접수 라벨로 렌더링한다")
    void list_rendersReceivedFilterAsReceptionLabel() throws Exception {
        AdminReservationListResponse viewModel = new AdminReservationListResponse(
                List.of(),
                List.of(
                        new AdminReservationStatusOptionResponse("ALL", "전체", "/admin/reservation/list?page=1&size=10&status=ALL", false),
                        new AdminReservationStatusOptionResponse("RECEIVED", "접수", "/admin/reservation/list?page=1&size=10&status=RECEIVED", true)
                ),
                List.of(new AdminReservationPageLinkResponse(1, "/admin/reservation/list?page=1&size=10&status=RECEIVED", true)),
                "RECEIVED",
                "",
                0,
                1,
                10,
                1,
                false,
                false,
                "",
                ""
        );
        given(adminReservationService.getReservationList(1, 10, "RECEIVED", null)).willReturn(viewModel);

        mockMvc.perform(get("/admin/reservation/list")
                        .param("status", "RECEIVED")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<option value=\"RECEIVED\" selected>접수</option>")));

        then(adminReservationService).should().getReservationList(1, 10, "RECEIVED", null);
    }

    @Test
    @DisplayName("예약 목록 관리 컬럼은 환자 상세 버튼만 노출한다")
    void list_rendersPatientDetailButtonInsteadOfCancelAction() throws Exception {
        AdminReservationItemResponse item = new AdminReservationItemResponse(
                12L,
                "RES-20260319-012",
                "2026-03-19",
                "10:30",
                7L,
                "/admin/patient/detail?patientId=7",
                "홍길동",
                "010-1234-5678",
                "내과",
                "김의사",
                "RECEIVED",
                "접수",
                false,
                true,
                false,
                false
        );
        AdminReservationListResponse viewModel = new AdminReservationListResponse(
                List.of(item),
                List.of(new AdminReservationStatusOptionResponse("RECEIVED", "접수", "/admin/reservation/list?page=1&size=10&status=RECEIVED", true)),
                List.of(new AdminReservationPageLinkResponse(1, "/admin/reservation/list?page=1&size=10&status=RECEIVED", true)),
                "RECEIVED",
                "",
                1,
                1,
                10,
                1,
                false,
                false,
                "",
                ""
        );
        given(adminReservationService.getReservationList(1, 10, "RECEIVED", null)).willReturn(viewModel);

        mockMvc.perform(get("/admin/reservation/list")
                        .param("status", "RECEIVED")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("환자 상세")))
                .andExpect(content().string(not(containsString("/admin/reservation/cancel"))))
                .andExpect(content().string(not(containsString("취소하시겠습니까?"))));

        then(adminReservationService).should().getReservationList(1, 10, "RECEIVED", null);
    }

    @Test
    @DisplayName("예약 목록은 keyword 값을 유지해 렌더링한다")
    void list_preservesKeywordOnRender() throws Exception {
        AdminReservationListResponse viewModel = new AdminReservationListResponse(
                List.of(),
                List.of(new AdminReservationStatusOptionResponse("ALL", "전체", "/admin/reservation/list?page=1&size=10&status=ALL&keyword=0101234", true)),
                List.of(new AdminReservationPageLinkResponse(1, "/admin/reservation/list?page=1&size=10&status=ALL&keyword=0101234", true)),
                "ALL",
                "0101234",
                0,
                1,
                10,
                1,
                false,
                false,
                "",
                ""
        );
        given(adminReservationService.getReservationList(1, 10, null, "0101234")).willReturn(viewModel);

        mockMvc.perform(get("/admin/reservation/list")
                        .param("keyword", "0101234")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(request().attribute("model", viewModel));

        then(adminReservationService).should().getReservationList(1, 10, null, "0101234");
    }

    private AdminReservationListResponse emptyResponse(String selectedStatus, String keyword) {
        return new AdminReservationListResponse(
                List.of(),
                List.of(new AdminReservationStatusOptionResponse("ALL", "전체", "/admin/reservation/list?page=1&size=10&status=ALL", true)),
                List.of(new AdminReservationPageLinkResponse(1, "/admin/reservation/list?page=1&size=10&status=ALL", true)),
                selectedStatus,
                keyword,
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
}
