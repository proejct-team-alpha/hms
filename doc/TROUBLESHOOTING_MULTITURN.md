# 트러블슈팅: 챗봇 멀티턴 대화 지원

## 배경

병원 규칙 Q&A 챗봇에서 "자연스러운 질의응답"이 이루어지지 않는 문제를 분석하고, 현재 구조 내에서 수정 가능한 범위를 파악하여 개선한 과정을 기록합니다.

---

## 문제 1: 스트리밍 경로에서 대화 이력(history)이 전달되지 않음

### 증상

- 챗봇에서 첫 질문 후 후속 질문을 하면 AI가 이전 대화를 전혀 기억하지 못함
- "당직 규정 알려줘" → "그거 더 자세히 알려줘" 시 "무엇에 대해 물어보시는 건가요?" 류의 응답 반환
- 폴백(non-stream) 경로에서는 맥락이 유지되는데 스트리밍에서만 발생

### 원인 분석

프론트엔드(JS)는 `chatHistory.slice(-6)`을 양쪽 경로 모두에 전송하고 있었지만, Spring Boot 스트리밍 경로에서 history를 누락하고 있었습니다.

**데이터 흐름 비교:**

| 경로 | 프론트 → Spring | Spring → Python | 결과 |
|------|----------------|-----------------|------|
| non-stream (`/query`) | query + history | query + history | 맥락 유지 |
| stream (`/query/stream`) | query + history | **query만 전달** | 매번 새 대화 |

**근본 원인:**

```java
// ChatService.java — history 파라미터 자체가 없었음
public Flux<String> callRuleLlmApiStream(String query) {
    return llmWebClient.post()
            .uri("/infer/rule/stream")
            .bodyValue(Map.of("query", query, "max_length", 1024, "temperature", 0.3))
            // ← history가 body에 포함되지 않음
```

```java
// ChatController.java — history를 서비스에 전달하지 않음
public Flux<String> handleRuleQueryStream(@RequestBody LlmRequest request) {
    return chatService.callRuleLlmApiStream(request.getQuery());
    // ← request.getHistory()를 사용하지 않음
}
```

Python 측은 이미 `body.history`를 수용하고 `_build_rule_messages()`에서 조합하고 있었으므로, Spring 측만 수정하면 되는 상황이었습니다.

### 해결

**ChatService.java:**
```java
public Flux<String> callRuleLlmApiStream(String query, List<Map<String, String>> history) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("query", query);
    body.put("max_length", 1024);
    body.put("temperature", 0.3);
    if (history != null && !history.isEmpty()) {
        body.put("history", history.subList(Math.max(0, history.size() - 6), history.size()));
    }
    // ...
}
```

**ChatController.java:**
```java
return chatService.callRuleLlmApiStream(request.getQuery(), request.getHistory());
```

### 검증 방법

- `compileJava` 빌드 성공 확인
- 브라우저에서 연속 질문 시 맥락 유지 확인

---

## 문제 2: 스트리밍 경로에서 대화 히스토리가 DB에 저장되지 않음

### 증상

- 스트리밍으로 대화한 내역이 `chatbot_history` 테이블에 기록되지 않음
- `/llm/chatbot/history/{staffId}` API에서 스트리밍 대화가 누락

### 원인 분석

non-stream 경로(`handleRuleQuery`)에는 `doOnNext`로 `saveChatHistory()`를 호출하는 로직이 있었으나, stream 경로(`handleRuleQueryStream`)에는 저장 로직이 전혀 없었습니다.

스트리밍은 Flux로 토큰이 여러 개로 나뉘어 오기 때문에, non-stream처럼 단순히 `doOnNext`에서 저장할 수 없고 전체 응답을 수집해야 합니다.

### 해결

`StringBuilder`로 스트림 토큰을 수집하고, `doOnComplete`에서 DB 저장:

```java
StringBuilder fullAnswer = new StringBuilder();

return chatService.callRuleLlmApiStream(request.getQuery(), request.getHistory())
        .doOnNext(fullAnswer::append)
        .doOnComplete(() -> {
            if (staffId != null && fullAnswer.length() > 0) {
                try {
                    chatService.saveChatHistory(staffId, effectiveSessionId,
                            request.getQuery(), fullAnswer.toString());
                } catch (Exception e) {
                    log.error("Rule Stream Q&A 히스토리 저장 실패", e);
                }
            }
        });
```

### 주의사항

- `doOnComplete`는 정상 완료 시에만 실행됨 (에러 시 저장 안 됨 — 의도된 동작)
- `StringBuilder`는 Reactor 파이프라인 외부에서 선언되므로 해당 요청 스코프에서만 유효

---

## 문제 3: 후속 질문 시 RAG 검색 결과가 비어 있음

### 증상

- "당직 규정 알려줘" → 정상 응답
- "그거 더 자세히 알려줘" → "검색된 규칙이 없습니다" 폴백 컨텍스트 사용 → 일반적인 안내만 제공

### 원인 분석

RAG 검색은 현재 쿼리 텍스트만으로 수행됩니다. "그거 더 자세히"에서 키워드를 추출하면 유의미한 검색어가 없어 MySQL LIKE 검색과 벡터 검색 모두 빈 결과를 반환합니다.

```
"그거 더 자세히 알려줘"
  → _extract_keywords() → ["자세히"] (나머지는 노이즈로 필터)
  → MySQL LIKE "%자세히%" → 0건
  → ChromaDB 벡터 검색 → distance > 0.5 (관련성 낮음) → 0건
```

LLM에 history가 전달되어 맥락은 이해하지만, RAG 컨텍스트가 없으므로 구체적인 규칙 내용을 답변에 포함하지 못합니다.

### 해결

`rule_context_service.py`에 후속 질문 감지 및 쿼리 확장 로직 추가:

**1단계: 후속 질문 감지 (`_is_followup_query`)**
```python
def _is_followup_query(query: str) -> bool:
    # 대명사 패턴: "그거", "아까", "더 자세히" 등
    # 짧은 질문: 10자 이하
    followup_patterns = ["그거", "그것", "더 자세히", "다른 건", "아까", ...]
    if len(query.strip()) <= 10:
        return True
    return any(p in query for p in followup_patterns)
```

**2단계: 이전 대화에서 키워드 추출하여 쿼리 확장 (`_expand_query_from_history`)**
```python
def _expand_query_from_history(query: str, history: list | None) -> str:
    if not _is_followup_query(query):
        return query  # 독립 질문은 확장하지 않음
    # 이전 user 메시지에서 키워드 추출 (최근 2턴)
    # 현재 쿼리에 없는 키워드만 최대 4개 추가
    # "그거 더 자세히 알려줘" → "그거 더 자세히 알려줘 당직"
```

**결과:**
```
"그거 더 자세히 알려줘" + history에서 "당직" 추출
  → 확장: "그거 더 자세히 알려줘 당직"
  → _extract_keywords() → ["자세히", "당직"]
  → MySQL LIKE "%당직%" → 당직 관련 규칙 히트
  → 벡터 검색도 "당직" 포함으로 관련 결과 반환
```

### 한계

- 키워드 확장은 휴리스틱 기반이므로 복잡한 맥락 전환에는 한계가 있음
- 예: "당직 규정 알려줘" → "위생은?" → "아까 말한 거" — "아까"가 당직인지 위생인지 판단 불가
- 이 경우 LLM이 history를 보고 맥락을 파악하지만, RAG 컨텍스트가 부정확할 수 있음

---

## 문제 4: 테스트 컴파일 실패

### 증상

```
ChatControllerTest.java:54: error: method callRuleLlmApi in class ChatService
cannot be applied to given types;
  required: String,List<Map<String,String>>
  found:    String
```

### 원인

`ChatService`의 메서드 시그니처에 `history` 파라미터를 추가했으나, 테스트 코드의 mock 설정이 이전 시그니처를 사용하고 있었습니다.

### 해결

```java
// Before
given(chatService.callRuleLlmApi(anyString())).willReturn(...)
given(chatService.callRuleLlmApiStream(anyString())).willReturn(...)

// After
given(chatService.callRuleLlmApi(anyString(), any())).willReturn(...)
given(chatService.callRuleLlmApiStream(anyString(), any())).willReturn(...)
```

### 교훈

서비스 메서드 시그니처 변경 시 반드시 `compileTestJava`까지 확인할 것. `compileJava`만으로는 테스트 코드 호환성을 검증하지 못합니다.

---

## 변경 파일 요약

| 파일 | 변경 내용 |
|------|-----------|
| `ChatService.java` | `callRuleLlmApiStream`에 history 파라미터 추가 |
| `ChatController.java` | 스트림 엔드포인트에 history 전달 + DB 저장 |
| `LlmRequest.java` | history 필드 추가 (이전 작업에서 완료) |
| `rule_context_service.py` | 후속 질문 감지 + 쿼리 확장 함수 추가 |
| `app.py` | `build_rule_context`에 history 전달 |
| `rule_system.txt` | 대화 맥락 규칙 3개 추가 |
| `ChatControllerTest.java` | mock 시그니처 수정 |
| `LLM_FEATURE_ARCHITECTURE.md` | 멀티턴 대화 지원 섹션 추가 |
