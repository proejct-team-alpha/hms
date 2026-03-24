# W4-8 Workflow — 통합 테스트 및 최종 검증

> **작성일**: 4W
> **브랜치**: `feature/Llm`
> **목표**: LLM 병합 결과물(Task 2~7)의 JUnit 슬라이스 테스트 작성 및 기존 테스트 통과 확인

---

## 전체 흐름

```
LlmPageControllerTest 작성
  → ChatControllerTest 작성
  → MedicalControllerTest 작성
  → LlmReservationControllerTest 작성
  → ./gradlew test 전체 통과 확인
  → LLM_SERVICE_URL 환경변수 설정 확인
  → 수동 시나리오 체크리스트 검증
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 테스트 패턴 | 기존 @WebMvcTest 슬라이스 + 내부 TestSecurityConfig 방식 준수 |
| LLM_SERVICE_URL | application-dev.properties 기본값 localhost:8000, 실제 서버 교체 필요 |
| 수동 시나리오 | 서버 기동 후 주요 API 직접 확인 (자동화 대상 아님) |
| 기존 테스트 회귀 | 150 tests BUILD SUCCESSFUL 확인 필요 |

---

## 실행 흐름

```
[1] LlmPageControllerTest 작성 — GET /llm/medical permitAll 200, GET /llm/chatbot 비인증 리다이렉트/인증 200
[2] ChatControllerTest 작성 — POST /llm/chatbot/query 비인증 리다이렉트/인증 200, GET 히스토리 인증 200
[3] MedicalControllerTest 작성 — POST /llm/medical/query 비인증 200, POST consult 비인증 200, GET 히스토리 인증 200
[4] LlmReservationControllerTest 작성 — GET /llm/reservation/slots/{doctorId} 비인증 200
[5] ./gradlew test — 전체 통과 확인 (기존 테스트 회귀 없음)
[6] LLM_SERVICE_URL 환경변수 확인 — .env.example에 추가
[7] 수동 시나리오 체크리스트 검증
```

---

## UI Mockup

```
[통합 테스트 작업 — UI 없음]

테스트 대상 API:
GET  /llm/medical              → permitAll 200
GET  /llm/chatbot              → 비인증 3xx, 인증 200
POST /llm/chatbot/query        → 비인증 3xx, 인증 200
GET  /llm/chatbot/history/{id} → 인증 200
POST /llm/medical/query        → permitAll 200
POST /llm/medical/query/consult→ permitAll 200
GET  /llm/medical/history/{id} → 인증 200
GET  /llm/reservation/slots/{doctorId} → permitAll 200
```

---

## 작업 목록

1. `LlmPageControllerTest` 작성 — GET /llm/medical permitAll 200, GET /llm/chatbot 비인증 리다이렉트/인증 200
2. `ChatControllerTest` 작성 — POST /llm/chatbot/query 비인증 리다이렉트/인증 200, GET /llm/chatbot/history/{staffId} 인증 200
3. `MedicalControllerTest` 작성 — POST /llm/medical/query 비인증 200, POST /llm/medical/query/consult 비인증 200, GET /llm/medical/history/{staffId} 인증 200
4. `LlmReservationControllerTest` 작성 — GET /llm/reservation/slots/{doctorId} 비인증 200, 응답 구조 확인
5. `./gradlew test` — 전체 통과 확인 (기존 테스트 회귀 없음)
6. `LLM_SERVICE_URL` 환경변수 확인 — .env.example에 추가 (http://192.168.0.73:8000)
7. 수동 시나리오 체크리스트 검증

---

## 작업 진행내용

- [x] LlmPageControllerTest 작성
- [x] ChatControllerTest 작성
- [x] MedicalControllerTest 작성
- [x] LlmReservationControllerTest 작성
- [x] 전체 테스트 통과 — 150 tests BUILD SUCCESSFUL
- [x] LLM_SERVICE_URL 환경변수 확인 — .env.example에 추가
- [ ] 수동 시나리오 체크리스트 (서버 기동 후 수동 확인)

---

## 실행 흐름에 대한 코드

### LLM 컨트롤러 공통 TestSecurityConfig

```java
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
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 테스트 전용 보안 설정 클래스입니다. 실제 운영 `SecurityConfig` 대신 테스트에서만 사용할 간단한 보안 규칙을 정의하고, 테스트 로그인용 더미 계정(`doctor` / `password`)을 메모리에 만들어 둡니다.
> - **왜 이렇게 썼는지**: `@WebMvcTest`는 컨트롤러 레이어만 테스트하는 슬라이스 테스트로, 실제 `SecurityConfig`와 DB 연결 없이 동작합니다. `@TestConfiguration`으로 테스트 환경 전용 설정을 별도로 만들면 실제 코드에 영향을 주지 않고 보안 동작을 테스트할 수 있습니다. `InMemoryUserDetailsManager`는 DB 없이 메모리에서만 사용자를 관리하는 간단한 인증 도구입니다.
> - **쉽게 말하면**: 실제 건물 보안 시스템 대신, 테스트용 모형 출입증 시스템을 설치해서 "이 문이 잠겨 있는지" 확인하는 것과 같습니다.

### LlmPageControllerTest 핵심 패턴

```java
@WebMvcTest(LlmPageController.class)
@Import(LlmPageControllerTest.TestSecurityConfig.class)
class LlmPageControllerTest {

    @Test
    void medicalPage_비인증_200() throws Exception {
        mockMvc.perform(get("/llm/medical"))
               .andExpect(status().isOk())
               .andExpect(view().name("llm/medical"));
    }

    @Test
    void chatbotPage_비인증_리다이렉트() throws Exception {
        mockMvc.perform(get("/llm/chatbot"))
               .andExpect(status().is3xxRedirection());
    }
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: `LlmPageController`만 테스트하는 슬라이스 테스트 클래스입니다. 첫 번째 테스트는 비로그인 상태로 `/llm/medical`에 접근했을 때 200 응답과 `llm/medical` 뷰가 반환되는지 확인합니다. 두 번째는 비로그인으로 `/llm/chatbot`에 접근하면 3xx 리다이렉트(로그인 페이지로 이동)가 일어나는지 확인합니다.
> - **왜 이렇게 썼는지**: `@WebMvcTest`는 전체 스프링 컨텍스트를 실행하지 않고 컨트롤러 관련 Bean만 로드해 빠르게 테스트합니다. `mockMvc.perform()`으로 실제 HTTP 요청 없이 테스트를 시뮬레이션하고, `andExpect()`로 기대하는 결과와 비교합니다.
> - **쉽게 말하면**: 실제 서버를 켜지 않고, "이 경로로 요청을 보내면 어떤 응답이 와야 하는가"를 가상으로 시험해 보는 것입니다.

### LLM_SERVICE_URL 환경변수 설정

```powershell
# run-dev.ps1 또는 IDE 실행 환경변수
$env:LLM_SERVICE_URL = "http://192.168.0.73:8000"
./gradlew bootRun --args='--spring.profiles.active=dev'
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: Windows PowerShell에서 환경변수 `LLM_SERVICE_URL`을 Python LLM 서버의 실제 주소로 설정하고, Spring Boot 앱을 개발 프로필(`dev`)로 실행합니다.
> - **왜 이렇게 썼는지**: `application-dev.properties`의 `llm.service.url=${LLM_SERVICE_URL:http://localhost:8000}`에서 `${LLM_SERVICE_URL:...}` 문법은 환경변수가 있으면 그 값을 쓰고 없으면 기본값(`localhost:8000`)을 씁니다. 개발자마다 LLM 서버 주소가 다를 수 있으므로 코드를 수정하지 않고 환경변수로 교체할 수 있습니다.
> - **쉽게 말하면**: 앱을 실행하기 전에 "AI 서버 주소는 이 IP야"라고 알려주는 것으로, 코드를 건드리지 않고 서버 주소를 바꿀 수 있습니다.

### 수동 시나리오 체크리스트

```
[ ] GET http://localhost:8080/llm/medical → AI 증상 상담 페이지 렌더링 (비인증 접근 가능)
[ ] GET http://localhost:8080/llm/chatbot → 미인증 시 /login 리다이렉트
[ ] GET http://localhost:8080/llm/chatbot (DOCTOR 로그인 후) → 병원규칙 Q&A 페이지 렌더링
[ ] POST /llm/medical/query {"query":"두통이 심해요"} → LLM 응답 텍스트 반환
[ ] POST /llm/medical/query/consult {"query":"소화불량"} → {generatedText, recommendedDepartment, doctors}
[ ] GET /llm/reservation/slots/1 → {doctorName, slots:[]} JSON 반환
[ ] POST /llm/chatbot/query (DOCTOR 세션) {"query":"당직 규정"} → 텍스트 응답 반환
[ ] GET /llm/chatbot/history/1 (DOCTOR 세션) → Page<ChatbotHistoryResponse> JSON 반환
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 자동화 테스트가 아닌, 개발자가 실제 서버를 켜고 직접 눈으로 확인해야 할 시나리오 목록입니다. 각 항목은 어떤 URL로 어떤 데이터를 보내면 어떤 결과가 나와야 하는지 기술합니다.
> - **왜 이렇게 썼는지**: JUnit 슬라이스 테스트는 Python LLM 서버 없이 Mock으로 동작하므로, 실제 AI 응답이 오는지는 자동 테스트로 확인할 수 없습니다. 수동 체크리스트로 실제 통합 동작을 사람이 직접 검증하는 단계가 필요합니다.
> - **쉽게 말하면**: 기계 검사(자동 테스트) 후에 직접 타고 운전해 보는 시승 테스트 목록으로, AI 서버와 실제로 연결되는지 사람이 직접 눈으로 확인합니다.

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| LlmPageControllerTest | @WebMvcTest 슬라이스 | 3개 GREEN |
| ChatControllerTest | @WebMvcTest 슬라이스 | 3개 GREEN |
| MedicalControllerTest | @WebMvcTest 슬라이스 | 3개 GREEN |
| LlmReservationControllerTest | @WebMvcTest 슬라이스 | 2개 GREEN |
| 전체 빌드 | ./gradlew test | 150 tests BUILD SUCCESSFUL |

---

## 완료 기준

- [x] `./gradlew test` 전체 통과 (기존 테스트 회귀 없음)
- [x] LLM 슬라이스 테스트 4종 GREEN
- [x] GET /llm/medical — 비인증 200
- [x] GET /llm/chatbot — 미인증 리다이렉트 / 인증 200
- [ ] POST /llm/medical/query — python-llm 서버 응답 수신 (수동 확인)
