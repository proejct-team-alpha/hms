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
