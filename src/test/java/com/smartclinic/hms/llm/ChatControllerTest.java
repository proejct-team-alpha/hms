package com.smartclinic.hms.llm;

import com.smartclinic.hms.auth.StaffRepository;
import com.smartclinic.hms.domain.ChatbotHistoryRepository;
import com.smartclinic.hms.llm.controller.ChatController;
import com.smartclinic.hms.llm.service.ChatService;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
@Import(ChatControllerTest.TestSecurityConfig.class)
class ChatControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ChatService chatService;

    @MockitoBean
    ChatbotHistoryRepository chatbotHistoryRepository;

    @MockitoBean
    StaffRepository staffRepository;

    @Test
    @DisplayName("POST /llm/chatbot/query - 비인증 접근 시 리다이렉트")
    void query_비인증_리다이렉트() throws Exception {
        mockMvc.perform(post("/llm/chatbot/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"당직 규정\"}"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("POST /llm/chatbot/query - DOCTOR 인증 후 200")
    void query_인증_200() throws Exception {
        given(chatService.callRuleLlmApi(any())).willReturn(Mono.just("당직은 월요일입니다."));
        given(staffRepository.findByUsernameAndActiveTrue(any())).willReturn(java.util.Optional.empty());

        mockMvc.perform(post("/llm/chatbot/query")
                        .with(user("doctor").roles("DOCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"당직 규정\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /llm/chatbot/query/stream - DOCTOR 인증 후 SSE 200")
    void queryStream_인증_200() throws Exception {
        given(chatService.callRuleLlmApiStream(any())).willReturn(Flux.just("data: 응답\n\n"));

        mockMvc.perform(post("/llm/chatbot/query/stream")
                        .with(user("doctor").roles("DOCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"당직 규정\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /llm/chatbot/history/{staffId} - DOCTOR 인증 후 Page 응답")
    void history_인증_200() throws Exception {
        given(chatbotHistoryRepository.findByStaff_IdOrderByCreatedAtDesc(any(), any()))
                .willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/llm/chatbot/history/1")
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
