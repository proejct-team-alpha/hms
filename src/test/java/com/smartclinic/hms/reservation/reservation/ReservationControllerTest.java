package com.smartclinic.hms.reservation.reservation;

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

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReservationController.class)
@Import(ReservationControllerTest.TestSecurityConfig.class)
class ReservationControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ReservationService reservationService;

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
