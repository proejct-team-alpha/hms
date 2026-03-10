<!-- Parent: ../AI-CONTEXT.md -->

# config — 스프링 설정

## 목적

애플리케이션 전역 설정. Security, MVC, Claude API, 에러 페이지, 속도 제한 설정을 담당한다.
**책임개발자 단독 소유 — 다른 팀원 수정 금지.**

## 주요 파일

| 파일 | 설명 |
|------|------|
| SecurityConfig.java | Spring Security FilterChain Bean 방식. 역할별 URL 접근 제어, CSRF 설정, 로그인/로그아웃 처리 |
| WebMvcConfig.java | LayoutModelInterceptor 등록, 정적 리소스 경로 설정 |
| ClaudeApiConfig.java | Claude API RestClient Bean 설정 (base URL, API key, timeout) |
| RateLimitFilter.java | 요청 속도 제한 필터 |
| ErrorPageController.java | 4xx/5xx 에러 페이지 렌더링 |

## AI 작업 지침

- **이 패키지 파일은 절대 수정하지 않는다**
- URL permitAll 추가가 필요하면 책임개발자에게 요청 (이슈/PR 코멘트)
- SecurityConfig의 `@Bean` 방식 FilterChain 패턴을 따름 (WebSecurityConfigurerAdapter 사용 금지)
- 테스트에서 Security 우회: `@WithMockUser` 사용

## 의존성

- 내부: `common/`, `domain/`
- 외부: Spring Security, Spring Web, Claude API (RestClient)
