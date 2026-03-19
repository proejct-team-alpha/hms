package com.smartclinic.hms.llm;

import com.smartclinic.hms.auth.StaffRepository;
import com.smartclinic.hms.domain.MedicalHistory;
import com.smartclinic.hms.domain.MedicalHistoryRepository;
import com.smartclinic.hms.llm.controller.MedicalController;
import com.smartclinic.hms.llm.dto.DoctorWithScheduleDto;
import com.smartclinic.hms.llm.service.DoctorService;
import com.smartclinic.hms.llm.service.LlmResponseParser;
import com.smartclinic.hms.llm.service.MedicalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MedicalController.class)
@Import(MedicalControllerTest.TestSecurityConfig.class)
class MedicalControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    MedicalService medicalService;

    @MockitoBean
    MedicalHistoryRepository medicalHistoryRepository;

    @MockitoBean
    DoctorService doctorService;

    @MockitoBean
    LlmResponseParser llmResponseParser;

    @MockitoBean
    StaffRepository staffRepository;

    @Test
    @DisplayName("POST /llm/medical/query - 비인증 접근 200 (permitAll)")
    void query_비인증_200() throws Exception {
        MedicalHistory mockHistory = mock(MedicalHistory.class);
        when(mockHistory.getId()).thenReturn(1L);
        given(medicalService.saveMedicalPending(any(), any())).willReturn(mockHistory);
        given(medicalService.callMedicalLlmApi(any())).willReturn(Mono.just("두통은 신경과 진료를 권장합니다."));

        mockMvc.perform(post("/llm/medical/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"두통이 심해요\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /llm/medical/query/consult - 비인증 접근 200 (permitAll)")
    void consultQuery_비인증_200() throws Exception {
        MedicalHistory mockHistory = mock(MedicalHistory.class);
        when(mockHistory.getId()).thenReturn(1L);
        given(medicalService.saveMedicalPending(any(), any())).willReturn(mockHistory);
        given(medicalService.callMedicalLlmApi(any())).willReturn(Mono.just("신경과 진료를 권장합니다."));
        given(llmResponseParser.extractDepartment(any())).willReturn("신경과");
        given(llmResponseParser.extractRecommendationReason(any())).willReturn("두통 증상 관련");
        given(doctorService.findDoctorsWithSchedule(any())).willReturn(List.<DoctorWithScheduleDto>of());

        MvcResult asyncResult = mockMvc.perform(post("/llm/medical/query/consult")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"두통이 심해요\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendedDepartment").value("신경과"));
    }

    @Test
    @DisplayName("POST /llm/medical/query/stream - 비인증 접근 200 SSE (permitAll)")
    void queryStream_비인증_200() throws Exception {
        given(medicalService.callMedicalLlmApiStream(any())).willReturn(Flux.just("data: 응답\n\n"));

        mockMvc.perform(post("/llm/medical/query/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"두통이 심해요\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /llm/medical/history/{staffId} - 인증 후 Page 응답")
    void history_인증_200() throws Exception {
        given(medicalHistoryRepository.findByStaff_IdOrderByCreatedAtDesc(any(), any()))
                .willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/llm/medical/history/1")
                        .with(user("doctor").roles("DOCTOR")))
                .andExpect(status().isOk());
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
