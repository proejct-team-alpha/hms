package com.smartclinic.hms.admin.reservation;

import com.smartclinic.hms.admin.reservation.dto.AdminReservationItemResponse;
import com.smartclinic.hms.admin.reservation.dto.AdminReservationListResponse;
import com.smartclinic.hms.admin.reservation.dto.AdminReservationPageLinkResponse;
import com.smartclinic.hms.admin.reservation.dto.AdminReservationStatusOptionResponse;
import com.smartclinic.hms.common.AdminControllerTestSecurityConfig;
import com.smartclinic.hms.common.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
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
        value = AdminReservationController.class,
        properties = {
                "spring.mustache.servlet.expose-request-attributes=true",
                "spring.mustache.servlet.allow-request-override=true"
        }
)
@Import(AdminControllerTestSecurityConfig.class)
class AdminReservationControllerTest {

    private static final String RESERVATION_CANCELLED_MESSAGE = "예약이 취소되었습니다.";
    private static final String RECEPTION_CANCELLED_MESSAGE = "접수가 취소되었습니다.";
    private static final String INVALID_STATUS_MESSAGE = "취소할 수 없는 상태입니다.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminReservationService adminReservationService;

    @Test
    @DisplayName("예약 목록은 기본 페이지와 pageTitle을 포함해 렌더링한다")
    void list_usesDefaultPagingAndRendersView() throws Exception {
        // given
        AdminReservationListResponse viewModel = emptyResponse("ALL", "");
        given(adminReservationService.getReservationList(1, 10, null, null)).willReturn(viewModel);

        // when
        // then
        mockMvc.perform(get("/admin/reservation/list")
                        .with(user("admin").roles("ADMIN"))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reservation-list"))
                .andExpect(request().attribute("model", viewModel))
                .andExpect(request().attribute("pageTitle", "예약 목록"))
                .andExpect(content().string(containsString("환자명 또는 연락처로 예약과 접수 현황을 빠르게 조회하고 관리합니다.")))
                .andExpect(content().string(containsString("placeholder=\"환자명 또는 연락처 검색\"")))
                .andExpect(content().string(containsString("조회")))
                .andExpect(content().string(containsString("초기화")));

        then(adminReservationService).should().getReservationList(1, 10, null, null);
    }

    @Test
    @DisplayName("예약 목록은 요청 파라미터를 서비스에 전달한다")
    void list_passesRequestParamsToService() throws Exception {
        // given
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

        // when
        // then
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
    @DisplayName("예약 목록은 RECEIVED 상태를 접수 라벨로 렌더링한다")
    void list_rendersReceivedFilterAsReceptionLabel() throws Exception {
        // given
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

        // when
        // then
        mockMvc.perform(get("/admin/reservation/list")
                        .param("status", "RECEIVED")
                .with(user("admin").roles("ADMIN"))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<option value=\"RECEIVED\" selected>접수</option>")));

        then(adminReservationService).should().getReservationList(1, 10, "RECEIVED", null);
    }

    @Test
    @DisplayName("예약 목록은 접수 상태 row를 접수 배지로 렌더링한다")
    void list_rendersReceivedReservationRowWithReceptionBadge() throws Exception {
        // given
        AdminReservationItemResponse receivedItem = new AdminReservationItemResponse(
                12L,
                "RES-20260319-012",
                "2026-03-19",
                "10:30",
                "홍만두",
                "010-1234-5678",
                "내과",
                "김의사",
                "RECEIVED",
                "접수",
                true,
                false,
                true,
                false,
                false
        );
        AdminReservationListResponse viewModel = new AdminReservationListResponse(
                List.of(receivedItem),
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

        // when
        // then
        mockMvc.perform(get("/admin/reservation/list")
                        .param("status", "RECEIVED")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("RES-20260319-012")))
                .andExpect(content().string(containsString("홍만두")))
                .andExpect(content().string(containsString(">접수</span>")))
                .andExpect(content().string(containsString("접수를 취소하시겠습니까?")));

        then(adminReservationService).should().getReservationList(1, 10, "RECEIVED", null);
    }

    @Test
    @DisplayName("예약 목록은 예약 상태 row를 예약 취소 문구로 렌더링한다")
    void list_rendersReservedReservationRowWithReservationCancelCopy() throws Exception {
        // given
        AdminReservationItemResponse reservedItem = new AdminReservationItemResponse(
                13L,
                "RES-20260319-013",
                "2026-03-19",
                "11:00",
                "김예약",
                "010-9876-5432",
                "외과",
                "박의사",
                "RESERVED",
                "예약",
                true,
                true,
                false,
                false,
                false
        );
        AdminReservationListResponse viewModel = new AdminReservationListResponse(
                List.of(reservedItem),
                List.of(new AdminReservationStatusOptionResponse("RESERVED", "예약", "/admin/reservation/list?page=1&size=10&status=RESERVED", true)),
                List.of(new AdminReservationPageLinkResponse(1, "/admin/reservation/list?page=1&size=10&status=RESERVED", true)),
                "RESERVED",
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
        given(adminReservationService.getReservationList(1, 10, "RESERVED", null)).willReturn(viewModel);

        // when
        // then
        mockMvc.perform(get("/admin/reservation/list")
                        .param("status", "RESERVED")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("RES-20260319-013")))
                .andExpect(content().string(containsString("김예약")))
                .andExpect(content().string(containsString(">예약</span>")))
                .andExpect(content().string(containsString("예약을 취소하시겠습니까?")));

        then(adminReservationService).should().getReservationList(1, 10, "RESERVED", null);
    }

    @Test
    @DisplayName("예약 목록은 keyword 파라미터를 그대로 유지해 렌더링한다")
    void list_preservesKeywordOnRender() throws Exception {
        // given
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

        // when
        // then
        mockMvc.perform(get("/admin/reservation/list")
                        .param("keyword", "0101234")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(request().attribute("model", viewModel));

        then(adminReservationService).should().getReservationList(1, 10, null, "0101234");
    }

    @Test
    @DisplayName("접수 취소 성공 시 서비스가 반환한 메시지로 목록에 복귀한다")
    void cancel_success_redirectsWithReturnedSuccessMessage() throws Exception {
        // given
        given(adminReservationService.cancelReservation(100L)).willReturn(RECEPTION_CANCELLED_MESSAGE);

        // when
        // then
        mockMvc.perform(post("/admin/reservation/cancel")
                        .param("reservationId", "100")
                        .param("page", "2")
                        .param("size", "10")
                        .param("status", "RECEIVED")
                        .param("keyword", "0101234")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("/admin/reservation/list?page=2&size=10&status=RECEIVED&keyword=0101234")))
                .andExpect(flash().attribute("successMessage", RECEPTION_CANCELLED_MESSAGE));

        then(adminReservationService).should().cancelReservation(100L);
    }

    @Test
    @DisplayName("예약 취소 실패 시 목록으로 리다이렉트하고 에러 메시지를 남긴다")
    void cancel_failure_redirectsWithErrorMessage() throws Exception {
        // given
        willThrow(new CustomException("INVALID_STATUS_TRANSITION", INVALID_STATUS_MESSAGE, HttpStatus.CONFLICT))
                .given(adminReservationService).cancelReservation(100L);

        // when
        // then
        mockMvc.perform(post("/admin/reservation/cancel")
                        .param("reservationId", "100")
                        .param("page", "2")
                        .param("size", "10")
                        .param("status", "COMPLETED")
                        .param("keyword", "0101234")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("/admin/reservation/list?page=2&size=10&status=COMPLETED&keyword=0101234")))
                .andExpect(flash().attribute("errorMessage", INVALID_STATUS_MESSAGE));

        then(adminReservationService).should().cancelReservation(100L);
    }

    @Test
    @DisplayName("예약 취소 시 잘못된 status 파라미터는 ALL로 정규화된다")
    void cancel_invalidStatus_redirectsWithAllFallback() throws Exception {
        // given
        given(adminReservationService.cancelReservation(100L)).willReturn(RESERVATION_CANCELLED_MESSAGE);

        // when
        // then
        mockMvc.perform(post("/admin/reservation/cancel")
                        .param("reservationId", "100")
                        .param("page", "3")
                        .param("size", "5")
                        .param("status", "INVALID")
                        .param("keyword", "0101234")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("/admin/reservation/list?page=3&size=5&status=ALL&keyword=0101234")))
                .andExpect(flash().attribute("successMessage", RESERVATION_CANCELLED_MESSAGE));

        then(adminReservationService).should().cancelReservation(100L);
    }

    @Test
    @DisplayName("예약 취소 시 소문자 status 파라미터는 대문자로 정규화된다")
    void cancel_lowercaseReceived_redirectsWithNormalizedStatus() throws Exception {
        // given
        given(adminReservationService.cancelReservation(100L)).willReturn(RECEPTION_CANCELLED_MESSAGE);

        // when
        // then
        mockMvc.perform(post("/admin/reservation/cancel")
                        .param("reservationId", "100")
                        .param("page", "1")
                        .param("size", "10")
                        .param("status", "received")
                        .param("keyword", "0101234")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("/admin/reservation/list?page=1&size=10&status=RECEIVED&keyword=0101234")))
                .andExpect(flash().attribute("successMessage", RECEPTION_CANCELLED_MESSAGE));

        then(adminReservationService).should().cancelReservation(100L);
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
