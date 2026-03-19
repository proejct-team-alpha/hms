package com.smartclinic.hms.llm;

import com.smartclinic.hms.llm.controller.LlmPageController;
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
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LlmPageController.class)
@Import(LlmPageControllerTest.TestSecurityConfig.class)
class LlmPageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("GET /llm/medical - 비인증 접근 200 (permitAll)")
    void medicalPage_비인증_200() throws Exception {
        mockMvc.perform(get("/llm/medical"))
                .andExpect(status().isOk())
                .andExpect(view().name("llm/medical"));
    }

    @Test
    @DisplayName("GET /llm/chatbot - 비인증 접근 시 /login 리다이렉트")
    void chatbotPage_비인증_리다이렉트() throws Exception {
        mockMvc.perform(get("/llm/chatbot"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("GET /llm/chatbot - DOCTOR 인증 후 200")
    void chatbotPage_인증_200() throws Exception {
        mockMvc.perform(get("/llm/chatbot")
                        .with(user("doctor").roles("DOCTOR")))
                .andExpect(status().isOk())
                .andExpect(view().name("llm/chatbot"));
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
