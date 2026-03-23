# W5-2 Workflow — ChatControllerTest / MedicalControllerTest / SymptomControllerTest

> **작성일**: 5W
> **브랜치**: `feature/reservation-Llm`
> **목표**: W4-8에서 누락된 테스트 2개 + W5-1 신규 컨트롤러 테스트 1개 작성

---

## 전체 흐름

```
ChatControllerTest 신규 작성 (4개 케이스)
  → MedicalControllerTest 신규 작성 (3개 케이스)
  → SymptomControllerTest 신규 작성 (2개 케이스)
  → ./gradlew test 전체 통과 확인
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 테스트 패턴 | 기존 @WebMvcTest 슬라이스 + 내부 TestSecurityConfig 방식 준수 |
| Mono<T> 비동기 반환 | andReturn() + asyncDispatch(result) 2단계 패턴 |
| W4-8 누락 | ChatControllerTest, MedicalControllerTest 미작성 상태 |
| SymptomController | W5-1 신규 추가됨 → 테스트 병행 작성 |

---

## 실행 흐름

```
[1] ChatControllerTest 신규 — @MockitoBean: ChatService, ChatbotHistoryRepository, StaffRepository
    - POST /llm/chatbot/query 비인증 리다이렉트
    - POST /llm/chatbot/query DOCTOR 인증 200 (Mono asyncDispatch 패턴)
    - POST /llm/chatbot/query/stream DOCTOR 인증 200 (Flux SSE)
    - GET /llm/chatbot/history/{staffId} DOCTOR 인증 200
[2] MedicalControllerTest 신규 — @MockitoBean: MedicalService, MedicalHistoryRepository,
    DoctorService, LlmResponseParser, StaffRepository
    - POST /llm/medical/query 비인증 200 (permitAll, asyncDispatch)
    - POST /llm/medical/query/consult 비인증 200 (asyncDispatch)
    - GET /llm/medical/history/{staffId} 인증 200
[3] SymptomControllerTest 신규 — @MockitoBean: SymptomAnalysisService
    - POST /llm/symptom/analyze 비인증 200 (permitAll)
    - 응답 구조 확인 — dept, doctor, time 필드
[4] ./gradlew test — 전체 통과 확인
```

---

## UI Mockup

```
[테스트 작업 — UI 없음]

신규 테스트 파일:
test/.../llm/ChatControllerTest.java
test/.../llm/MedicalControllerTest.java
test/.../llm/SymptomControllerTest.java
```

---

## 작업 목록

1. `ChatControllerTest` 신규 — @MockitoBean: ChatService, ChatbotHistoryRepository, StaffRepository (4개 케이스)
2. `MedicalControllerTest` 신규 — @MockitoBean: MedicalService, MedicalHistoryRepository, DoctorService, LlmResponseParser, StaffRepository (3개 케이스)
3. `SymptomControllerTest` 신규 — @MockitoBean: SymptomAnalysisService (2개 케이스)
4. `./gradlew test` — 전체 통과 확인

---

## 작업 진행내용

- [x] ChatControllerTest 작성
- [x] MedicalControllerTest 작성
- [x] SymptomControllerTest 작성
- [x] 전체 테스트 통과 — BUILD SUCCESSFUL

---

## 실행 흐름에 대한 코드

### Mono<T> asyncDispatch 2단계 패턴

```java
// Mono 비동기 컨트롤러 테스트 패턴
@Test
void query_인증_200() throws Exception {
    given(chatService.query(any(), any())).willReturn(Mono.just("응답 텍스트"));

    MvcResult result = mockMvc.perform(post("/llm/chatbot/query")
            .with(user("doctor").roles("DOCTOR"))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"query\":\"당직\"}"))
            .andExpect(request().asyncStarted())
            .andReturn();

    mockMvc.perform(asyncDispatch(result))
            .andExpect(status().isOk());
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: `Mono<T>`를 반환하는 비동기 컨트롤러를 테스트할 때 사용하는 2단계 패턴입니다. 1단계에서 비동기 작업이 시작됐는지 확인하고(`asyncStarted`), 2단계에서 비동기 결과를 처리한 뒤 최종 상태 코드를 검증합니다.
> - **왜 이렇게 썼는지**: `Mono<T>`는 WebFlux의 비동기 타입으로, 일반적인 동기 응답과 달리 처리가 끝날 때까지 기다려야 합니다. `andReturn()`으로 먼저 비동기 시작 상태를 받고, `asyncDispatch(result)`로 비동기 완료 후 결과를 디스패치해야 실제 응답 코드를 검증할 수 있습니다. `given(...).willReturn(Mono.just(...))`는 실제 서비스 대신 가짜 응답을 주도록 설정하는 Mockito 문법입니다.
> - **쉽게 말하면**: 배달 완료를 두 번에 걸쳐 확인하는 것처럼, "배달 시작했어요" 확인 → "배달 완료됐어요" 확인 순서로 테스트를 진행합니다.

### SymptomControllerTest

```java
@WebMvcTest(SymptomController.class)
@Import(SymptomControllerTest.TestSecurityConfig.class)
class SymptomControllerTest {

    @MockitoBean
    SymptomAnalysisService symptomAnalysisService;

    @Test
    void analyze_비인증_200() throws Exception {
        given(symptomAnalysisService.analyzeSymptom(any()))
                .willReturn(new SymptomResponse("내과", "의사이영희", "09:00"));

        mockMvc.perform(post("/llm/symptom/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"symptomText\":\"두통\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dept").value("내과"))
                .andExpect(jsonPath("$.doctor").value("의사이영희"))
                .andExpect(jsonPath("$.time").value("09:00"));
    }
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: `SymptomController`만 단독으로 테스트하는 슬라이스 테스트입니다. 실제 AI 서비스 대신 가짜(`@MockitoBean`) 서비스를 주입하고, `POST /llm/symptom/analyze`로 요청을 보냈을 때 응답 JSON의 각 필드 값이 올바른지 검증합니다.
> - **왜 이렇게 썼는지**: `@WebMvcTest`는 웹 레이어(컨트롤러)만 로드하는 가벼운 테스트 방식입니다. `@MockitoBean`으로 실제 서비스 구현체 없이 가짜 응답을 설정합니다. `jsonPath("$.dept")`는 응답 JSON에서 `dept` 필드 값을 꺼내 비교하는 표현식입니다. `@Import(TestSecurityConfig.class)`는 테스트용 보안 설정을 적용합니다.
> - **쉽게 말하면**: 실제 AI 호출 없이 "컨트롤러가 요청을 올바르게 처리하고 응답을 돌려주는가?"만 빠르게 확인하는 모의 테스트입니다.

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| ChatControllerTest | @WebMvcTest 슬라이스 | 4개 GREEN |
| MedicalControllerTest | @WebMvcTest 슬라이스 | 3개 GREEN |
| SymptomControllerTest | @WebMvcTest 슬라이스 | 2개 GREEN |
| 전체 빌드 | ./gradlew test | BUILD SUCCESSFUL |

---

## 완료 기준

- [x] ChatControllerTest 4개 GREEN
- [x] MedicalControllerTest 3개 GREEN
- [x] SymptomControllerTest 2개 GREEN
- [x] `./gradlew test` 전체 통과
