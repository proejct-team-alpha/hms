# W5-3 Report — SymptomAnalysisService python-llm 서버 경유로 변경

> **작성일**: 2026-03-19
> **브랜치**: `feature/reservation-Llm`
> **빌드**: BUILD SUCCESSFUL

---

## 작업 완료 목록

| # | 항목 | 상태 |
|---|------|------|
| 1 | `SymptomAnalysisService` — `claudeRestClient` → `llmWebClient`, `Mono<SymptomResponse>` 반환 | ✅ |
| 2 | `SymptomController` — `SymptomResponse` → `Mono<SymptomResponse>` 반환 | ✅ |
| 3 | `SymptomControllerTest` — `Mono.just()` mock + `asyncDispatch` 패턴 적용 | ✅ |
| 4 | `./gradlew test` 전체 통과 | ✅ |

---

## 변경 내용

### SymptomAnalysisService

| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| 의존성 | `RestClient claudeRestClient` | `WebClient llmWebClient` |
| 제거된 의존성 | `@Value("${claude.api.model}")` | — |
| 반환 타입 | `SymptomResponse` | `Mono<SymptomResponse>` |
| LLM 엔드포인트 | `POST /v1/messages` (Anthropic) | `POST /infer/medical` (python-llm) |
| 오류 처리 | `try/catch → DEFAULT_RESPONSE` | `onErrorResume → Mono.just(DEFAULT_RESPONSE)` |

---

## LLM 연동 방식 통일 현황

| 기능 | 경로 | LLM 연동 방식 |
|------|------|---------------|
| AI 증상 분석 | `/llm/symptom/analyze` | **python-llm** `/infer/medical` ✅ |
| 의료 상담 | `/llm/medical/query` | **python-llm** `/infer/medical` |
| 병원규칙 Q&A | `/llm/chatbot/query` | **python-llm** `/infer/rule` |
