package com.smartclinic.hms.reservation.reservation;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.smartclinic.hms.common.exception.CustomException;
import java.util.Optional;

@WebMvcTest(ReservationController.class)
@Import(ReservationControllerTest.TestSecurityConfig.class)
class ReservationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ReservationService reservationService;

    @Test
    @DisplayName("예약 생성 성공 - PRG 패턴으로 /reservation/complete 리다이렉트")
    void createReservation_success_redirectsToComplete() throws Exception {
        // given
        given(reservationService.createReservation(any()))
                .willReturn(new ReservationCompleteInfo(
                        "RES-20260401-001", "홍길동", "내과", "김내과", "2026-04-01", "09:00"));

        // when & then
        mockMvc.perform(post("/reservation/create")
                .with(csrf())
                .param("name", "홍길동")
                .param("phone", "01012345678")
                .param("departmentId", "1")
                .param("doctorId", "1")
                .param("reservationDate", "2026-04-01")
                .param("timeSlot", "09:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/reservation/complete*"));
    }

    @Test
    @DisplayName("@Valid 검증 실패 - 이름 빈 값 시 폼 뷰 재표시 및 에러 메시지")
    void createReservation_blankName_returnsFormWithError() throws Exception {
        // when & then
        mockMvc.perform(post("/reservation/create")
                .with(csrf())
                .param("name", "")
                .param("phone", "01012345678")
                .param("departmentId", "1")
                .param("doctorId", "1")
                .param("reservationDate", "2026-04-01")
                .param("timeSlot", "09:00"))
                .andExpect(status().isOk())
                .andExpect(view().name("reservation/direct-reservation"))
                .andExpect(request().attribute("errorMessage", "이름을 입력해주세요."));
    }

    @Test
    @DisplayName("@Valid 검증 실패 - 전화번호 빈 값 시 폼 뷰 재표시 및 에러 메시지")
    void createReservation_blankPhone_returnsFormWithError() throws Exception {
        // when & then
        mockMvc.perform(post("/reservation/create")
                .with(csrf())
                .param("name", "홍길동")
                .param("phone", "")
                .param("departmentId", "1")
                .param("doctorId", "1")
                .param("reservationDate", "2026-04-01")
                .param("timeSlot", "09:00"))
                .andExpect(status().isOk())
                .andExpect(view().name("reservation/direct-reservation"))
                // phone: @NotBlank + @Pattern 가 동시에 걸릴 수 있어 메시지 결합 순서는 구현·버전에 따라 달라짐
                .andExpect(request().attribute("errorMessage", anyOf(
                        containsString("연락처를 입력해주세요."),
                        containsString("연락처 형식이 올바르지 않습니다."))));
    }

    @Test
    @DisplayName("예약 생성 성공 - flash attribute로 DTO 전달, URL에 파라미터 없음")
    void createReservation_success_setsFlashAttributeWithDto() throws Exception {
        // given
        given(reservationService.createReservation(any()))
                .willReturn(new ReservationCompleteInfo(
                        "RES-20260401-001", "홍길동", "내과", "김내과", "2026-04-01", "09:00"));

        // when & then
        mockMvc.perform(post("/reservation/create")
                .with(csrf())
                .param("name", "홍길동")
                .param("phone", "01012345678")
                .param("departmentId", "1")
                .param("doctorId", "1")
                .param("reservationDate", "2026-04-01")
                .param("timeSlot", "09:00"))
                .andExpect(status().is3xxRedirection())
                // URL에 쿼리 파라미터 없이 정확히 /reservation/complete 로만 리다이렉트
                .andExpect(redirectedUrl("/reservation/complete"))
                // flash attribute로 ReservationCompleteInfo DTO가 통째로 전달됨
                .andExpect(flash().attributeExists("info"));
    }

    @Test
    @DisplayName("예약 취소 성공 - flash attribute로 DTO 전달, cancel-complete 리다이렉트")
    void cancelReservation_success_setsFlashAttributeAndRedirects() throws Exception {
        // given
        given(reservationService.cancelReservation(anyLong(), anyString()))
                .willReturn(new ReservationCompleteInfo(
                        "RES-20260401-001", "홍길동", "내과", "김내과", "2026-04-01", "09:00"));

        // when & then
        mockMvc.perform(post("/reservation/cancel/1")
                .with(csrf())
                .param("phone", "01012345678"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reservation/cancel-complete"))
                // flash attribute로 ReservationCompleteInfo DTO가 통째로 전달됨
                .andExpect(flash().attributeExists("info"));
    }

    @Test
    @DisplayName("예약 취소 실패 - 전화번호 불일치 시 취소 화면 재표시 및 에러 메시지")
    void cancelReservation_wrongPhone_returnsFormWithError() throws Exception {
        // given - H-01: 소유권 검증 실패 시 CustomException.forbidden 발생
        given(reservationService.cancelReservation(anyLong(), anyString()))
                .willThrow(CustomException.forbidden("예약 소유자가 아닙니다."));

        // when & then
        mockMvc.perform(post("/reservation/cancel/1")
                .with(csrf())
                .param("phone", "01099999999"))
                .andExpect(status().isOk())
                .andExpect(view().name("reservation/reservation-cancel"))
                .andExpect(request().attribute("errorMessage", "예약 소유자가 아닙니다."));
    }

    @Test
    @DisplayName("예약 변경 성공 - flash attribute로 DTO 전달, modify-complete 리다이렉트")
    void modifyReservation_success_setsFlashAttributeAndRedirects() throws Exception {
        // given
        given(reservationService.updateReservation(anyLong(), anyString(), any()))
                .willReturn(new ReservationCompleteInfo(
                        "RES-20260402-001", "홍길동", "내과", "김내과", "2026-04-02", "10:00"));

        // when & then
        mockMvc.perform(post("/reservation/modify/1")
                .with(csrf())
                .param("phone", "01012345678")
                .param("departmentId", "1")
                .param("doctorId", "1")
                .param("reservationDate", "2026-04-02")
                .param("timeSlot", "10:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reservation/modify-complete"))
                // flash attribute로 ReservationCompleteInfo DTO가 통째로 전달됨
                .andExpect(flash().attributeExists("info"));
    }

    @Test
    @DisplayName("@Valid 검증 실패 - 예약 변경 시간 빈 값 시 폼 뷰 재표시 및 에러 메시지")
    void modifyReservation_blankTimeSlot_returnsFormWithError() throws Exception {
        // given - 유효성 검사 실패 시 기존 예약 재조회 → Optional.empty() 처리
        given(reservationService.findById(anyLong())).willReturn(Optional.empty());

        // when & then
        mockMvc.perform(post("/reservation/modify/1")
                .with(csrf())
                .param("phone", "01012345678")
                .param("departmentId", "1")
                .param("doctorId", "1")
                .param("reservationDate", "2026-04-02")
                .param("timeSlot", ""))  // @NotBlank 위반
                .andExpect(status().isOk())
                .andExpect(view().name("reservation/reservation-modify"))
                .andExpect(request().attribute("errorMessage", "예약 시간을 선택해주세요."));
    }

    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .csrf(csrf -> csrf.disable())
                    .build();
        }
    }
}
