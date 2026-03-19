# W4-8 통합 테스트 및 최종 검증

## 작업 목표
LLM 병합 결과물(Task 2~7)의 JUnit 슬라이스 테스트 작성 및 기존 테스트 통과 확인.
python-llm 서버(IP:8000)를 활용한 수동 시나리오 체크리스트로 최종 검증.

## 작업 목록

1. `LlmPageControllerTest` 작성 — `GET /llm/medical` permitAll 200, `GET /llm/chatbot` 비인증 리다이렉트 / 인증 200
2. `ChatControllerTest` 작성 — `POST /llm/chatbot/query` 비인증 리다이렉트 / 인증 200, `GET /llm/chatbot/history/{staffId}` 인증 200
3. `MedicalControllerTest` 작성 — `POST /llm/medical/query` 비인증 200, `POST /llm/medical/query/consult` 비인증 200, `GET /llm/medical/history/{staffId}` 인증 200
4. `LlmReservationControllerTest` 작성 — `GET /llm/reservation/slots/{doctorId}` 비인증 200, 응답 구조(`doctorName`, `slots`) 확인
5. `./gradlew test` — 전체 통과 확인 (기존 테스트 회귀 없음)
6. `LLM_SERVICE_URL` 환경변수 설정 확인 — `application-dev.properties` 기본값 `localhost:8000`, 실제 서버 `192.168.0.73:8000`으로 교체
7. 수동 시나리오 체크리스트 검증 — 서버 기동 후 주요 API 직접 확인

## 진행 현황
- [x] 1. LlmPageControllerTest
- [x] 2. ChatControllerTest
- [x] 3. MedicalControllerTest
- [x] 4. LlmReservationControllerTest
- [x] 5. 전체 테스트 통과 — 150 tests BUILD SUCCESSFUL
- [x] 6. LLM_SERVICE_URL 환경변수 확인 — .env.example에 추가 (http://192.168.0.73:8000)
- [ ] 7. 수동 시나리오 체크리스트 (서버 기동 후 수동 확인)

## 신규 테스트 파일

- `test/.../llm/LlmPageControllerTest.java`
- `test/.../llm/ChatControllerTest.java`
- `test/.../llm/MedicalControllerTest.java`
- `test/.../llm/LlmReservationControllerTest.java`

---

## 상세 내용

### 테스트 공통 패턴

기존 테스트 패턴 준수:
- `@WebMvcTest(대상Controller.class)` — MVC 슬라이스
- 내부 `@TestConfiguration` + `TestSecurityConfig` — 테스트용 Security 설정
- `@MockitoBean` — 서비스/레포지토리 mock
- `@Import(TestClass.TestSecurityConfig.class)`

**LLM 컨트롤러 공통 TestSecurityConfig**
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

### 1. LlmPageControllerTest

대상: `LlmPageController` (GET /llm/medical, GET /llm/chatbot)

| 테스트 | 조건 | 기대 결과 |
|---|---|---|
| medicalPage_비인증_200 | 인증 없이 GET /llm/medical | 200 OK, 뷰 `llm/medical` |
| chatbotPage_비인증_리다이렉트 | 인증 없이 GET /llm/chatbot | 3xx → /login |
| chatbotPage_인증_200 | DOCTOR 인증 후 GET /llm/chatbot | 200 OK, 뷰 `llm/chatbot` |

### 2. ChatControllerTest

대상: `ChatController` (POST /llm/chatbot/query, GET /llm/chatbot/history/{staffId})

MockBean 목록: `ChatService`, `ChatbotHistoryRepository`, `StaffRepository`

| 테스트 | 조건 | 기대 결과 |
|---|---|---|
| query_비인증_403 | 인증 없이 POST /llm/chatbot/query | 3xx 또는 401/403 |
| query_인증_200 | DOCTOR 인증 + `{"query":"당직"}` | 200 OK |
| history_인증_200 | DOCTOR 인증 + GET /llm/chatbot/history/1 | 200 OK, Page JSON |

### 3. MedicalControllerTest

대상: `MedicalController` (POST /llm/medical/query/consult, GET /llm/medical/history/{staffId})

MockBean 목록: `MedicalService`, `MedicalHistoryRepository`, `DoctorService`, `LlmResponseParser`, `StaffRepository`

| 테스트 | 조건 | 기대 결과 |
|---|---|---|
| query_비인증_200 | 인증 없이 POST /llm/medical/query | 200 OK (permitAll) |
| consultQuery_비인증_200 | 인증 없이 POST /llm/medical/query/consult | 200 OK |
| history_인증_200 | DOCTOR 인증 + GET /llm/medical/history/1 | 200 OK, Page JSON |

### 4. LlmReservationControllerTest

대상: `LlmReservationController` (GET /llm/reservation/slots/{doctorId})

MockBean 목록: `LlmReservationService`

| 테스트 | 조건 | 기대 결과 |
|---|---|---|
| slots_비인증_200 | 인증 없이 GET /llm/reservation/slots/1 | 200 OK (permitAll) |
| slots_응답_구조 | mock 반환값 설정 | doctorName, slots 필드 확인 |

### 5. LLM_SERVICE_URL 환경변수 설정

`application-dev.properties` 기본값: `http://localhost:8000`
python-llm 서버가 외부 IP:8000으로 구동 중이므로 실행 시 환경변수 지정:

```powershell
# run-dev.ps1 또는 IDE 실행 환경변수
$env:LLM_SERVICE_URL = "http://<IP>:8000"
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### 6. 수동 시나리오 체크리스트

```
[ ] GET http://localhost:8080/llm/medical
    → AI 증상 상담 페이지 렌더링 (비인증 접근 가능)

[ ] GET http://localhost:8080/llm/chatbot
    → 미인증 시 /login 리다이렉트

[ ] GET http://localhost:8080/llm/chatbot (DOCTOR 로그인 후)
    → 병원규칙 Q&A 페이지 렌더링

[ ] POST http://localhost:8080/llm/medical/query
    Content-Type: application/json
    {"query": "두통이 심해요"}
    → LLM 응답 텍스트 반환 (python-llm 연결 확인)

[ ] POST http://localhost:8080/llm/medical/query/consult
    {"query": "소화불량이 있어요"}
    → {generatedText, recommendedDepartment, doctors} JSON 반환

[ ] GET http://localhost:8080/llm/reservation/slots/1
    → {doctorName, slots:[]} JSON 반환

[ ] POST http://localhost:8080/llm/chatbot/query (DOCTOR 세션)
    {"query": "당직 규정 알려줘"}
    → 텍스트 응답 반환

[ ] GET http://localhost:8080/llm/chatbot/history/1 (DOCTOR 세션)
    → Page<ChatbotHistoryResponse> JSON 반환
```

## 수용 기준
- [ ] `./gradlew test` 전체 통과 (기존 테스트 회귀 없음)
- [ ] LLM 슬라이스 테스트 4종 GREEN
- [ ] GET /llm/medical — 비인증 200
- [ ] GET /llm/chatbot — 미인증 리다이렉트 / 인증 200
- [ ] POST /llm/medical/query — python-llm 서버 응답 수신
