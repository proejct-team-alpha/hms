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

### LLM_SERVICE_URL 환경변수 설정

```powershell
# run-dev.ps1 또는 IDE 실행 환경변수
$env:LLM_SERVICE_URL = "http://192.168.0.73:8000"
./gradlew bootRun --args='--spring.profiles.active=dev'
```

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
