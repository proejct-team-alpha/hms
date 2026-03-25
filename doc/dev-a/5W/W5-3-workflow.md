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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 변경 전에는 Anthropic Claude API에 직접 동기(기다리는) 방식으로 호출했고, 변경 후에는 내부 python-llm 서버에 비동기(기다리지 않는) 방식으로 호출합니다. `Mono<SymptomResponse>`는 "나중에 완성될 하나의 결과"를 의미하는 비동기 타입입니다.
> - **왜 이렇게 썼는지**: `WebClient`는 Spring WebFlux의 비동기 HTTP 클라이언트로, 서버 자원을 효율적으로 사용합니다. `Map.of()`로 요청 파라미터를 간결하게 구성하고, `.bodyToMono()`로 응답을 객체로 변환합니다. `.onErrorMap()`은 통신 오류 시 애플리케이션용 예외로 변환해 일관된 오류 처리를 가능하게 합니다. `temperature`는 AI 답변의 창의성 정도를 조절하는 값입니다(낮을수록 일관된 답변).
> - **쉽게 말하면**: 외부 유명 AI 서비스에 직접 전화하던 것을, 사내 AI 서버에 메시지를 보내는 방식으로 바꾼 것입니다. 결과는 나중에 와도 괜찮은 비동기 방식으로 처리합니다.

### SymptomController — Mono 반환

```java
@PostMapping("/analyze")
public Mono<SymptomResponse> analyze(@RequestBody SymptomRequest request) {
    return symptomAnalysisService.analyzeSymptom(request.getSymptomText());
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 컨트롤러의 응답 타입을 `SymptomResponse`(즉시 반환)에서 `Mono<SymptomResponse>`(비동기 반환)로 변경합니다. Spring WebFlux가 이 `Mono`를 자동으로 처리해 실제 값이 준비되면 클라이언트에게 응답을 보냅니다.
> - **왜 이렇게 썼는지**: 서비스 레이어가 `Mono<SymptomResponse>`를 반환하도록 바뀌었으므로, 컨트롤러도 동일한 타입을 반환해야 비동기 흐름이 유지됩니다. Spring MVC는 `Mono` 타입을 인식하고 비동기로 처리합니다.
> - **쉽게 말하면**: 창구 직원(컨트롤러)이 "지금 바로 결과 드릴게요"에서 "잠시 기다리시면 결과 드릴게요" 방식으로 바뀐 것입니다.

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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 서비스가 `Mono<SymptomResponse>`를 반환하도록 바뀌었으므로, 테스트의 mock 설정도 `Mono.just(...)`로 감싸서 가짜 응답을 설정합니다. 그리고 W5-2에서 배운 asyncDispatch 2단계 패턴을 그대로 적용합니다.
> - **왜 이렇게 썼는지**: `Mono.just(value)`는 이미 완성된 값 하나를 즉시 담고 있는 `Mono`를 만듭니다. 비동기 컨트롤러는 `andReturn()` → `asyncDispatch()` 순서로 테스트해야 실제 응답을 검증할 수 있습니다. `any()`는 어떤 값이 인자로 와도 이 mock이 동작하도록 하는 Mockito 매처입니다.
> - **쉽게 말하면**: "무슨 증상을 물어봐도 '내과 / 의사이영희 / 09:00'이라고 답하는 가짜 AI"를 만들어서 컨트롤러의 동작만 검증합니다.

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
