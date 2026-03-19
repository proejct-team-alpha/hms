package com.smartclinic.hms.admin.reservation;

import com.smartclinic.hms.admin.reservation.dto.AdminReservationListResponse;
import com.smartclinic.hms.admin.reservation.dto.AdminReservationPageLinkResponse;
import com.smartclinic.hms.admin.reservation.dto.AdminReservationStatusOptionResponse;
import com.smartclinic.hms.common.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.smartclinic.hms.common.AdminControllerTestSecurityConfig;
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

    private static final String CANCEL_SUCCESS_MESSAGE = "예약이 취소되었습니다.";
    private static final String INVALID_STATUS_MESSAGE = "취소할 수 없는 상태입니다.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminReservationService adminReservationService;

    @Test
    @DisplayName("예약 목록은 기본 페이징과 pageTitle을 포함해 렌더링한다")
    void list_usesDefaultPagingAndRendersView() throws Exception {
        // given
        AdminReservationListResponse viewModel = new AdminReservationListResponse(
                List.of(),
                List.of(new AdminReservationStatusOptionResponse("ALL", "전체", "/admin/reservation/list?page=1&size=10&status=ALL", true)),
                List.of(new AdminReservationPageLinkResponse(1, "/admin/reservation/list?page=1&size=10&status=ALL", true)),
                "ALL",
                0,
                1,
                10,
                1,
                false,
                false,
                "",
                ""
        );
        given(adminReservationService.getReservationList(1, 10, null)).willReturn(viewModel);

        // when
        // then
        mockMvc.perform(get("/admin/reservation/list")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reservation-list"))
                .andExpect(request().attribute("model", viewModel))
                .andExpect(request().attribute("pageTitle", "예약 목록"))
                .andExpect(content().string(containsString("예약과 접수 현황을 상태별로 확인하고 관리합니다.")))
                .andExpect(content().string(containsString("상태 필터")))
                .andExpect(content().string(containsString("조회된 내역이 없습니다.")));

        then(adminReservationService).should().getReservationList(1, 10, null);
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
                0,
                2,
                5,
                0,
                true,
                false,
                "/admin/reservation/list?page=1&size=5&status=RESERVED",
                ""
        );
        given(adminReservationService.getReservationList(2, 5, "RESERVED")).willReturn(viewModel);

        // when
        // then
        mockMvc.perform(get("/admin/reservation/list")
                        .param("page", "2")
                        .param("size", "5")
                        .param("status", "RESERVED")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reservation-list"))
                .andExpect(request().attribute("model", viewModel))
                .andExpect(request().attribute("pageTitle", "예약 목록"));

        then(adminReservationService).should().getReservationList(2, 5, "RESERVED");
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
                0,
                1,
                10,
                1,
                false,
                false,
                "",
                ""
        );
        given(adminReservationService.getReservationList(1, 10, "RECEIVED")).willReturn(viewModel);

        // when
        // then
        mockMvc.perform(get("/admin/reservation/list")
                        .param("status", "RECEIVED")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">접수</a>")));

        then(adminReservationService).should().getReservationList(1, 10, "RECEIVED");
    }

    @Test
    @DisplayName("예약 취소 성공 시 목록으로 리다이렉트하고 성공 메시지를 남긴다")
    void cancel_success_redirectsWithSuccessMessage() throws Exception {
        // when
        // then
        mockMvc.perform(post("/admin/reservation/cancel")
                        .param("reservationId", "100")
                        .param("page", "2")
                        .param("size", "10")
                        .param("status", "RECEIVED")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("/admin/reservation/list?page=2&size=10&status=RECEIVED")))
                .andExpect(flash().attribute("successMessage", CANCEL_SUCCESS_MESSAGE));

        then(adminReservationService).should().cancelReservation(100L);
    }

    @Test
    @DisplayName("예약 취소 실패 시 목록으로 리다이렉트하고 오류 메시지를 남긴다")
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
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("/admin/reservation/list?page=2&size=10&status=COMPLETED")))
                .andExpect(flash().attribute("errorMessage", INVALID_STATUS_MESSAGE));

        then(adminReservationService).should().cancelReservation(100L);
    }

    @Test
    @DisplayName("예약 취소 후 잘못된 status 파라미터는 ALL로 정규화된다")
    void cancel_invalidStatus_redirectsWithAllFallback() throws Exception {
        // when
        // then
        mockMvc.perform(post("/admin/reservation/cancel")
                        .param("reservationId", "100")
                        .param("page", "3")
                        .param("size", "5")
                        .param("status", "INVALID")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("/admin/reservation/list?page=3&size=5&status=ALL")))
                .andExpect(flash().attribute("successMessage", CANCEL_SUCCESS_MESSAGE));

        then(adminReservationService).should().cancelReservation(100L);
    }

    @Test
    @DisplayName("예약 취소 후 소문자 status 파라미터도 정상적으로 복귀한다")
    void cancel_lowercaseReceived_redirectsWithNormalizedStatus() throws Exception {
        // when
        // then
        mockMvc.perform(post("/admin/reservation/cancel")
                        .param("reservationId", "100")
                        .param("page", "1")
                        .param("size", "10")
                        .param("status", "received")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("/admin/reservation/list?page=1&size=10&status=RECEIVED")))
                .andExpect(flash().attribute("successMessage", CANCEL_SUCCESS_MESSAGE));

        then(adminReservationService).should().cancelReservation(100L);
    }

}
