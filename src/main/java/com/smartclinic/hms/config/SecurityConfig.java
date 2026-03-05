package com.smartclinic.hms.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

/**
 * ════════════════════════════════════════════════════════════════════════════
 * Spring Security 설정 — 세션 기반 인증·인가
 * ════════════════════════════════════════════════════════════════════════════
 *
 * ■ 인증 방식: 세션 기반 (Spring Security 기본 세션 — JWT 미사용)
 * ■ UserDetailsService: CustomUserDetailsService (Staff DB 조회)
 * ■ 역할: ROLE_ADMIN, ROLE_DOCTOR, ROLE_NURSE, ROLE_STAFF
 *
 * ■ URL 접근 권한 (API 명세서 v3.0 §1.3)
 *
 *   경로                  대상               인증
 *   ─────────────────     ────────────────   ─────────────────────────────
 *   /                     비회원 메인        불필요
 *   /reservation/**       비회원 환자        불필요
 *   /llm/symptom/**       비회원 증상 분석   불필요 (§4)
 *   /llm/rules/**         내부 직원 챗봇     ROLE_DOCTOR, ROLE_NURSE (§8)
 *   /staff/**             접수 직원          ROLE_STAFF, ROLE_ADMIN (§5)
 *   /doctor/**            의사               ROLE_DOCTOR, ROLE_ADMIN (§6)
 *   /nurse/**             간호사             ROLE_NURSE, ROLE_ADMIN (§7)
 *   /admin/**             관리자             ROLE_ADMIN 전용 (§9~§14)
 *
 * ■ 보안 설정
 *   - CORS: 허용 Origin 화이트리스트 (환경변수 설정 가능)
 *   - CSRF: SSR 폼 보호 활성화, /llm/symptom/** 만 제외
 *   - Security Headers: CSP, HSTS, Referrer-Policy, Permissions-Policy
 *   - Rate Limiting: RateLimitFilter (IP 기반 토큰 버킷)
 *   - 로그인 실패: 사용자 열거 방지 (일관된 에러 메시지)
 * ════════════════════════════════════════════════════════════════════════════
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** CORS 허용 Origin — 환경변수 또는 기본값 (개발: localhost:8080) */
    @Value("${hms.cors.allowed-origins:http://localhost:8080}")
    private String allowedOrigins;

    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(RateLimitFilter rateLimitFilter) {
        this.rateLimitFilter = rateLimitFilter;
    }

    // ════════════════════════════════════════════════════════════════════════
    // H2 Console Security Filter Chain (개발용)
    // ════════════════════════════════════════════════════════════════════════

    @Bean
    @Order(1)
    SecurityFilterChain h2ConsoleFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher("/h2-console/**")
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .build();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Main Security Filter Chain
    // ════════════════════════════════════════════════════════════════════════

    @Bean
    @Order(2)
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            // ── CORS ────────────────────────────────────────────────────
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ── CSRF ────────────────────────────────────────────────────
            // SSR(Mustache) 폼 제출 보호 활성화.
            // 비회원 LLM 증상 분석 AJAX(JSON 전용)만 제외.
            // /llm/rules/** 는 인증 필요 엔드포인트이므로 CSRF 보호 유지.
            // JS fetch 호출 시 CSRF 토큰을 X-CSRF-TOKEN 헤더로 전송 필요.
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/llm/symptom/**")
            )

            // ── Security Headers ────────────────────────────────────────
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; " +
                                      "script-src 'self' 'unsafe-inline' https://cdn.tailwindcss.com https://unpkg.com; " +
                                      "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                                      "font-src 'self' https://fonts.gstatic.com; " +
                                      "img-src 'self' data:; " +
                                      "frame-ancestors 'self'")
                )
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                )
                .referrerPolicy(referrer -> referrer
                    .policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
            )

            // ── Rate Limiting Filter ────────────────────────────────────
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)

            // ── URL별 접근 권한 ───────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                // 정적 리소스
                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                // Spring MVC 에러 페이지
                .requestMatchers("/error/**").permitAll()
                // 샘플 참고 코드 (개발용)
                .requestMatchers("/sample/**").permitAll()

                // ── 인증 화면 (§2) ──────────────────────────────────────
                .requestMatchers("/login", "/logout").permitAll()

                // ── 비회원 메인·외부 예약 (§3) ──────────────────────────
                .requestMatchers("/", "/reservation/**").permitAll()

                // ── LLM 증상 분석 — 비회원 AJAX (§4) ───────────────────
                .requestMatchers("/llm/symptom/**").permitAll()

                // ── LLM 규칙 챗봇 — 내부 직원 AJAX (§8) ────────────────
                .requestMatchers("/llm/rules/**").hasAnyRole("DOCTOR", "NURSE")

                // ── 접수 직원 (§5) ───────────────────────────────────────
                .requestMatchers("/staff/**").hasAnyRole("STAFF", "ADMIN")

                // ── 의사 (§6) ────────────────────────────────────────────
                .requestMatchers("/doctor/**").hasAnyRole("DOCTOR", "ADMIN")

                // ── 간호사 (§7) ──────────────────────────────────────────
                .requestMatchers("/nurse/**").hasAnyRole("NURSE", "ADMIN")

                // ── 관리자 (§9~§14) ──────────────────────────────────────
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // 그 외 모든 요청 — 인증 필요
                .anyRequest().authenticated()
            )

            // ── 폼 로그인 ─────────────────────────────────────────────────
            .formLogin(form -> form
                .loginPage("/login")                       // GET /login — 로그인 화면 (§2.1)
                .loginProcessingUrl("/login")              // POST /login — 로그인 처리 (§2.2)
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(roleBasedSuccessHandler()) // 역할별 대시보드 리다이렉트
                .failureHandler(loginFailureHandler())     // 사용자 열거 방지 — 일관된 에러 메시지
                .permitAll()
            )

            // ── 로그아웃 ──────────────────────────────────────────────────
            .logout(logout -> logout
                .logoutUrl("/logout")                      // POST /logout — 로그아웃 처리 (§2.3)
                .logoutSuccessUrl("/login?logout=true")    // 성공 후 (§2.3)
                .invalidateHttpSession(true)               // 세션 무효화
                .deleteCookies("JSESSIONID")               // 세션 쿠키 삭제
                .permitAll()
            )

            // ── 세션 관리 ─────────────────────────────────────────────────
            .sessionManagement(session -> {
                session.maximumSessions(1)
                       .maxSessionsPreventsLogin(false);
            })

            // ── 인증·인가 오류 처리 (§1.6) ───────────────────────────────
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/error/403")
            )

            .build();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Beans
    // ════════════════════════════════════════════════════════════════════════

    /**
     * 로그인 성공 시 ROLE별 대시보드로 리다이렉트 (API 명세서 §2.2).
     */
    @Bean
    AuthenticationSuccessHandler roleBasedSuccessHandler() {
        return new SimpleUrlAuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request,
                                                HttpServletResponse response,
                                                Authentication authentication) throws IOException {
                String targetUrl = resolveTargetUrl(authentication);
                getRedirectStrategy().sendRedirect(request, response, targetUrl);
            }

            private String resolveTargetUrl(Authentication authentication) {
                for (GrantedAuthority authority : authentication.getAuthorities()) {
                    return switch (authority.getAuthority()) {
                        case "ROLE_ADMIN"  -> "/admin/index";
                        case "ROLE_DOCTOR" -> "/doctor/dashboard";
                        case "ROLE_NURSE"  -> "/nurse/dashboard";
                        case "ROLE_STAFF"  -> "/staff/dashboard";
                        default            -> "/";
                    };
                }
                return "/";
            }
        };
    }

    /**
     * 로그인 실패 핸들러 — 사용자 열거 방지 (§2.2)
     * 아이디 미존재 / 비밀번호 불일치 / 비활성 계정 모두 동일 에러 파라미터 전달.
     */
    @Bean
    SimpleUrlAuthenticationFailureHandler loginFailureHandler() {
        return new SimpleUrlAuthenticationFailureHandler("/login?error=true");
    }

    /**
     * BCrypt 패스워드 인코더.
     * Staff 등록(§10.3) 및 비밀번호 수정 시 암호화에 사용.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS 설정 — 허용 Origin 화이트리스트.
     * 환경변수 hms.cors.allowed-origins 로 쉼표 구분 Origin 지정 가능.
     * 기본값: http://localhost:8080 (개발용)
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "X-CSRF-TOKEN", "Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * 세션 동시성 제어를 위한 이벤트 퍼블리셔.
     * sessionManagement().maximumSessions() 동작에 필수.
     */
    @Bean
    HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
