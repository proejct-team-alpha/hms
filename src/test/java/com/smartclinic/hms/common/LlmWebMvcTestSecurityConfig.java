package com.smartclinic.hms.common;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * LLM·증상 분석 등 {@code /llm/**} WebMvcTest 에서 공통으로 쓰는 보안 스텁 (M-06).
 * 실제 {@link com.smartclinic.hms.config.SecurityConfig} 와 유사한 URL 규칙만 최소 반영한다.
 */
@TestConfiguration
@EnableWebSecurity
public class LlmWebMvcTestSecurityConfig {

    @Bean
    SecurityFilterChain llmTestSecurityFilterChain(HttpSecurity http) throws Exception {
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
    UserDetailsService llmTestUserDetailsService() {
        return new InMemoryUserDetailsManager(
                User.withUsername("doctor").password("{noop}password").roles("DOCTOR").build());
    }
}
