package com.smartclinic.hms.llm;

import com.smartclinic.hms.llm.controller.LlmReservationController;
import com.smartclinic.hms.llm.dto.LlmReservationResponse;
import com.smartclinic.hms.llm.service.LlmReservationService;
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
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LlmReservationController.class)
@Import(LlmReservationControllerTest.TestSecurityConfig.class)
class LlmReservationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    LlmReservationService llmReservationService;

    @Test
    @DisplayName("GET /llm/reservation/slots/{doctorId} - 비인증 접근 200 (permitAll)")
    void slots_비인증_200() throws Exception {
        given(llmReservationService.getAvailableSlots(anyLong()))
                .willReturn(new LlmReservationResponse.SlotList(1L, "김신경", List.of()));

        mockMvc.perform(get("/llm/reservation/slots/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /llm/reservation/slots/{doctorId} - 응답 구조 확인")
    void slots_응답구조() throws Exception {
        given(llmReservationService.getAvailableSlots(anyLong()))
                .willReturn(new LlmReservationResponse.SlotList(1L, "김신경", List.of()));

        mockMvc.perform(get("/llm/reservation/slots/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.doctorId").value(1))
                .andExpect(jsonPath("$.doctorName").value("김신경"))
                .andExpect(jsonPath("$.slots").isArray());
    }

    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/llm/medical/**", "/llm/reservation/**").permitAll()
                            .requestMatchers("/llm/chatbot/**").authenticated()
                            .anyRequest().authenticated())
                    .formLogin(form -> form.loginPage("/login").permitAll())
                    .csrf(csrf -> csrf.ignoringRequestMatchers(
                            "/llm/medical/**", "/llm/chatbot/**", "/llm/reservation/**"))
                    .build();
        }

        @Bean
        UserDetailsService userDetailsService() {
            return new InMemoryUserDetailsManager(
                    User.withUsername("doctor").password("{noop}password").roles("DOCTOR").build());
        }
    }
}
