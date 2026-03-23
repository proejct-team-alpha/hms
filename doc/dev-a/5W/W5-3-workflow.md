# W5-3 Workflow — SymptomAnalysisService python-llm 서버 경유로 변경

> **작성일**: 5W
> **브랜치**: `feature/reservation-Llm`
> **목표**: Claude API → python-llm `/infer/medical` 경유로 변경, 전체 LLM 연동 방식 통일

---

## 전체 흐름

```
SymptomAnalysisService 수정 (claudeRestClient → llmWebClient)
  → 반환 타입 SymptomResponse → Mono<SymptomResponse>
  → SymptomController 반환 타입 수정
  → SymptomControllerTest asyncDispatch 패턴 적용
  → ./gradlew test 전체 통과 확인
```

---

## 인터뷰 결과

| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| LLM 연동 | `RestClient claudeRestClient` (Anthropic API) | `WebClient llmWebClient` (python-llm) |
| 엔드포인트 | `POST /v1/messages` | `POST /infer/medical` |
| 반환 타입 | `SymptomResponse` (동기) | `Mono<SymptomResponse>` (비동기) |
| 파싱 실패 처리 | 기본값 반환 | `LlmServiceUnavailableException` throw |

---

## 실행 흐름

```
[1] SymptomAnalysisService 수정
    - RestClient claudeRestClient → WebClient llmWebClient 교체
    - @Value("${claude.api.model}") 제거
    - analyzeSymptom() 반환 타입 Mono<SymptomResponse>로 변경
    - POST /infer/medical 호출 (query, max_length=64, temperature=0.1)
    - LlmResponse.generatedText 정규식 파싱 유지
    - 파싱 실패 시 LlmServiceUnavailableException throw
[2] SymptomController 수정 — 반환 타입 Mono<SymptomResponse>로 변경
[3] SymptomControllerTest 수정 — Mono.just() mock + asyncDispatch 2단계 패턴 적용
[4] ./gradlew test — 전체 통과 확인
```

---

## UI Mockup

```
[Service 내부 변경 — UI 없음]

LLM 연동 경로 통일:
기존: SymptomAnalysisService → Anthropic API (POST /v1/messages)
변경: SymptomAnalysisService → python-llm (POST /infer/medical)
                              ↑ ChatService, MedicalService와 동일한 경로
```

---

## 작업 목록

1. `SymptomAnalysisService` 수정 — `llmWebClient` 교체, `Mono<SymptomResponse>` 반환, 파싱 실패 시 `LlmServiceUnavailableException`
2. `SymptomController` 수정 — 반환 타입 `Mono<SymptomResponse>`로 변경
3. `SymptomControllerTest` 수정 — `Mono.just()` mock + asyncDispatch 2단계 패턴 적용
4. `./gradlew test` — 전체 통과 확인

---

## 작업 진행내용

- [x] SymptomAnalysisService 수정 (llmWebClient, Mono 반환)
- [x] SymptomController 수정 (Mono 반환 타입)
- [x] SymptomControllerTest 수정 (asyncDispatch 패턴)
- [x] 전체 테스트 통과 — BUILD SUCCESSFUL

---

## 실행 흐름에 대한 코드

### SymptomAnalysisService — llmWebClient 전환

```java
// 변경 전
@RequiredArgsConstructor
public class SymptomAnalysisService {
    private final RestClient claudeRestClient;
    @Value("${claude.api.model}") private String model;

    public SymptomResponse analyzeSymptom(String symptomText) {
        // POST /v1/messages 동기 호출
    }
}

// 변경 후
@RequiredArgsConstructor
public class SymptomAnalysisService {
    private final WebClient llmWebClient;

    public Mono<SymptomResponse> analyzeSymptom(String symptomText) {
        return llmWebClient.post()
                .uri("/infer/medical")
                .bodyValue(Map.of(
                    "query", symptomText,
                    "max_length", 64,
                    "temperature", 0.1
                ))
                .retrieve()
                .bodyToMono(LlmResponse.class)
                .map(resp -> parseResponse(resp.getGeneratedText()))
                .onErrorMap(e -> new LlmServiceUnavailableException("python-llm 서버 연결 실패", e));
    }
}
```

### SymptomController — Mono 반환

```java
@PostMapping("/analyze")
public Mono<SymptomResponse> analyze(@RequestBody SymptomRequest request) {
    return symptomAnalysisService.analyzeSymptom(request.getSymptomText());
}
```

### SymptomControllerTest — asyncDispatch 패턴 적용

```java
@Test
void analyze_비인증_200() throws Exception {
    given(symptomAnalysisService.analyzeSymptom(any()))
            .willReturn(Mono.just(new SymptomResponse("내과", "의사이영희", "09:00")));

    MvcResult result = mockMvc.perform(post("/llm/symptom/analyze")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"symptomText\":\"두통\"}"))
            .andExpect(request().asyncStarted())
            .andReturn();

    mockMvc.perform(asyncDispatch(result))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dept").value("내과"));
}
```

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| SymptomControllerTest | asyncDispatch 패턴 | 2개 GREEN |
| 전체 빌드 | ./gradlew test | BUILD SUCCESSFUL |
| LLM 연동 방식 | ChatService/MedicalService와 동일 | llmWebClient 사용 확인 |

---

## 완료 기준

- [x] SymptomAnalysisService — llmWebClient 사용, `Mono<SymptomResponse>` 반환
- [x] SymptomController — `Mono<SymptomResponse>` 반환
- [x] SymptomControllerTest — 수정 후 GREEN
- [x] `./gradlew test` 전체 통과
