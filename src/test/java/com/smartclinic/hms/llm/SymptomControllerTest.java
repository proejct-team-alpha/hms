package com.smartclinic.hms.llm;

import com.smartclinic.hms.llm.controller.SymptomController;
import com.smartclinic.hms.llm.dto.SymptomResponse;
import com.smartclinic.hms.llm.service.SymptomAnalysisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
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
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SymptomController.class)
@Import(SymptomControllerTest.TestSecurityConfig.class)
class SymptomControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SymptomAnalysisService symptomAnalysisService;

    @Test
    @DisplayName("POST /llm/symptom/analyze - 비인증 200 (permitAll)")
    void analyze_비인증_200() throws Exception {
        given(symptomAnalysisService.analyzeSymptom(anyString()))
                .willReturn(Mono.just(new SymptomResponse("내과", "의사이영희", "09:00")));

        MvcResult result = mockMvc.perform(post("/llm/symptom/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symptomText\":\"열이 나요\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /llm/symptom/analyze - 응답 구조 확인 (dept, doctor, time)")
    void analyze_응답구조() throws Exception {
        given(symptomAnalysisService.analyzeSymptom(anyString()))
                .willReturn(Mono.just(new SymptomResponse("소아과", "의사최지우", "11:00")));

        MvcResult result = mockMvc.perform(post("/llm/symptom/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symptomText\":\"아이가 열이 나요\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dept").value("소아과"))
                .andExpect(jsonPath("$.doctor").value("의사최지우"))
                .andExpect(jsonPath("$.time").value("11:00"));
    }

    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/llm/medical/**", "/llm/reservation/**", "/llm/symptom/**").permitAll()
                            .requestMatchers("/llm/chatbot/**").authenticated()
                            .anyRequest().authenticated())
                    .formLogin(form -> form.loginPage("/login").permitAll())
                    .csrf(csrf -> csrf.ignoringRequestMatchers(
                            "/llm/medical/**", "/llm/chatbot/**", "/llm/reservation/**", "/llm/symptom/**"))
                    .build();
        }

        @Bean
        UserDetailsService userDetailsService() {
            return new InMemoryUserDetailsManager(
                    User.withUsername("doctor").password("{noop}password").roles("DOCTOR").build());
        }
    }
}
