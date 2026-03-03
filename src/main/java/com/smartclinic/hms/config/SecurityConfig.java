package com.smartclinic.hms.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import java.io.IOException;

/**
 * ════════════════════════════════════════════════════════════════════════════
 * Spring Security 설정 — 세션 기반 인증·인가
 * ════════════════════════════════════════════════════════════════════════════
 *
 * ■ 인증 방식: 세션 기반 (Spring Security 기본 세션 — JWT 미사용)
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
 * ■ 로그인 성공 리다이렉트 (§2.2)
 *   ROLE_ADMIN  → /admin/dashboard
 *   ROLE_DOCTOR → /doctor/dashboard
 *   ROLE_NURSE  → /nurse/dashboard
 *   ROLE_STAFF  → /staff/dashboard
 *
 * ■ UserDetailsService
 *   InMemoryUserDetailsManager (테스트: admin01, staff01, doctor01, nurse01 / password123)
 * ════════════════════════════════════════════════════════════════════════════
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // ════════════════════════════════════════════════════════════════════════
    // H2 Console Security Filter Chain (개발용)
    // ════════════════════════════════════════════════════════════════════════
    // H2 콘솔은 별도 서블릿이므로 MvcRequestMatcher로 매칭 불가.
    // PathRequest.toH2Console()을 사용하는 전용 필터 체인 필요.

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
            // ── CSRF ─────────────────────────────────────────────────────
            // SSR(Mustache) 폼 제출 보호 활성화.
            // LLM AJAX 엔드포인트(JSON 전용)는 제외 — JS fetch 호출 시 CSRF 헤더 불필요.
            // 폼 제출 엔드포인트는 Mustache 템플릿에 {{_csrf.token}} 포함 필요.
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/llm/**")
            )

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
                .failureUrl("/login?error=true")           // 실패 시 (§2.2)
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
            // 동일 계정 최대 세션 1개.
            // maxSessionsPreventsLogin(false): 새 로그인 시 기존 세션 만료 (타 기기 자동 로그아웃).
            // HttpSessionEventPublisher Bean 등록 필수 (아래 참고).
            .sessionManagement(session -> {
                session.maximumSessions(1)
                       .maxSessionsPreventsLogin(false);
            })

            // ── 인증·인가 오류 처리 (§1.6) ───────────────────────────────
            // 미로그인 접근(401): Spring Security가 자동으로 /login 리다이렉트.
            // 권한 없는 접근(403): /error/403 화면 렌더링.
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/error/403")
            )

            // ── 보안 헤더 ─────────────────────────────────────────────────
            // H2 콘솔이 iframe을 사용하므로 sameOrigin 허용 (개발용).
            // 운영 환경에서 H2 콘솔을 비활성화하면 기본값(DENY)으로 복원 권장.
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            )

            .build();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Beans
    // ════════════════════════════════════════════════════════════════════════

    /**
     * 로그인 성공 시 ROLE별 대시보드로 리다이렉트 (API 명세서 §2.2).
     *
     * <pre>
     * ROLE_ADMIN  → /admin/dashboard
     * ROLE_DOCTOR → /doctor/dashboard
     * ROLE_NURSE  → /nurse/dashboard
     * ROLE_STAFF  → /staff/dashboard
     * </pre>
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
                        case "ROLE_ADMIN"  -> "/admin/dashboard";
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
     * BCrypt 패스워드 인코더.
     * Staff 등록(§10.3) 및 비밀번호 수정 시 암호화에 사용.
     * UserDetailsService 구현체에서 주입받아 사용.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    InMemoryUserDetailsManager userDetailsService(PasswordEncoder encoder) {
        UserDetails admin = User.builder().username("admin01").password(encoder.encode("password123")).roles("ADMIN").build();
        UserDetails staff = User.builder().username("staff01").password(encoder.encode("password123")).roles("STAFF").build();
        UserDetails doctor = User.builder().username("doctor01").password(encoder.encode("password123")).roles("DOCTOR").build();
        UserDetails nurse = User.builder().username("nurse01").password(encoder.encode("password123")).roles("NURSE").build();
        return new InMemoryUserDetailsManager(admin, staff, doctor, nurse);
    }

    /**
     * 세션 동시성 제어를 위한 이벤트 퍼블리셔.
     * sessionManagement().maximumSessions() 동작에 필수.
     * 세션 생성·소멸 이벤트를 Spring Security가 감지하여 동시 세션을 관리함.
     */
    @Bean
    HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
