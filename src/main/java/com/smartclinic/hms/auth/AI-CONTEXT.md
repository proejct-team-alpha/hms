<!-- Parent: ../AI-CONTEXT.md -->

# auth — 인증

## 목적

Spring Security 기반 직원 로그인/로그아웃 처리. 세션 기반 인증.

## 주요 파일

| 파일 | 설명 |
|------|------|
| AuthController.java | GET /login, POST /login (Security가 처리), GET /logout |
| CustomUserDetailsService.java | `UserDetailsService` 구현. username → Staff 엔티티 → UserDetails 변환 |
| StaffRepository.java | `findByUsername(String)` — 로그인 시 직원 조회 |

## AI 작업 지침

- 로그인 POST 처리는 Spring Security가 담당 (`SecurityConfig`의 `formLogin()` 설정)
- 인증 후 역할별 리다이렉트: `SecurityConfig`의 `defaultSuccessUrl` 또는 `AuthenticationSuccessHandler`
- 비밀번호 인코더: `BCryptPasswordEncoder` (SecurityConfig Bean)

## 의존성

- 내부: `domain/Staff`, `domain/StaffRole`
- 외부: Spring Security, Spring Data JPA
