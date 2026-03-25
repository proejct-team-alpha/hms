# W5-3 Report — SymptomAnalysisService python-llm 서버 경유로 변경

> **작성일**: 2026-03-19
> **브랜치**: `feature/reservation-Llm`
> **빌드**: `./gradlew test --rerun` BUILD SUCCESSFUL

---

## 작업 완료 목록

| # | 항목 | 상태 |
|---|------|------|
| 1 | `SymptomAnalysisService` — `claudeRestClient` → `llmWebClient`, `Mono<SymptomResponse>` 반환 | ✅ |
| 2 | `SymptomController` — `SymptomResponse` → `Mono<SymptomResponse>` 반환 | ✅ |
| 3 | `symptom-reservation.mustache` — 더미 제거, 실제 fetch + CSRF + 로딩 UX | ✅ |
| 4 | `SymptomControllerTest` — `Mono.just()` mock + `asyncDispatch` 패턴 | ✅ |
| 5 | `./gradlew test --rerun` 전체 통과 | ✅ |

---

## 변경 내용 상세

### SymptomAnalysisService

| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| 의존성 | `RestClient claudeRestClient` | `WebClient llmWebClient` |
| `@Value` | `${claude.api.model}` | 제거 |
| 반환 타입 | `SymptomResponse` (동기) | `Mono<SymptomResponse>` (비동기) |
| LLM 엔드포인트 | `POST /v1/messages` (Anthropic) | `POST /infer/medical` (python-llm) |
| max_length | 200 | 64 |
| 오류 처리 | `onErrorResume → DEFAULT_RESPONSE` | `LlmServiceUnavailableException` throw |
| doctor 파싱 | 원문 그대로 | 대괄호 제거 (`[김철수]` → `김철수`) |
| time 파싱 | 필수 (null → 기본값 반환) | 선택 (null → `"09:00"` 기본값) |

### symptom-reservation.mustache

| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| `callSymptomApi` | 더미 (setTimeout + 로컬 키워드 매핑) | 실제 `fetch POST /llm/symptom/analyze` |
| CSRF | 미처리 | `meta[name="_csrf"]` 토큰 전송 |
| 로딩 UX | "AI가 분석 중입니다..." 고정 | 4단계 메시지 순환 (1.8초 간격) |
| 샘플 데이터 | `SYMPTOM_MAP` 키워드 매핑 하드코딩 | 제거 |

**로딩 메시지 순환:**
```
증상을 분석하고 있습니다...
진료과를 찾고 있습니다...
전문의를 확인하고 있습니다...
예약 가능 시간을 조회하고 있습니다...
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: AI 분석 중 사용자에게 표시되는 로딩 메시지 4단계를 정의합니다. 1.8초 간격으로 순환하며 표시해 사용자가 기다리는 동안 진행 상황을 알 수 있도록 합니다.
> - **왜 이렇게 썼는지**: AI 서버 응답에는 수 초가 걸릴 수 있어, 단순히 "분석 중..."만 고정으로 표시하면 사용자가 화면이 멈췄다고 오해할 수 있습니다. 분석 단계를 순차적으로 보여줌으로써 실제로 처리가 진행 중임을 느낄 수 있게 하는 UX(사용자 경험) 기법입니다.
> - **쉽게 말하면**: 식당에서 요리 중일 때 "재료 준비 중 → 조리 중 → 플레이팅 중"처럼 진행 상황을 보여주는 것과 같습니다.

---

## LLM 연동 방식 통일 현황

| 기능 | 경로 | LLM 연동 방식 |
|------|------|---------------|
| AI 증상 분석 | `/llm/symptom/analyze` | **python-llm** `/infer/medical` ✅ |
| 의료 상담 | `/llm/medical/query` | **python-llm** `/infer/medical` |
| 병원규칙 Q&A | `/llm/chatbot/query` | **python-llm** `/infer/rule` |
