# W5-3 Workflow — SymptomAnalysisService python-llm 서버 경유로 변경

> **작성일**: 2026-03-19
> **브랜치**: `feature/reservation-Llm`
> **목표**: Claude API → python-llm `/infer/medical` 경유로 변경, 전체 LLM 연동 방식 통일

---

## 변경 전 / 후

| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| LLM 연동 | `RestClient claudeRestClient` (Anthropic API) | `WebClient llmWebClient` (python-llm) |
| 엔드포인트 | `POST /v1/messages` | `POST /infer/medical` |
| 반환 타입 | `SymptomResponse` (동기) | `Mono<SymptomResponse>` (비동기) |

---

## 작업 목록

1. `SymptomAnalysisService` 수정
   - `RestClient claudeRestClient` → `WebClient llmWebClient` 교체
   - `@Value("${claude.api.model}")` 제거
   - `analyzeSymptom()` 반환 타입: `SymptomResponse` → `Mono<SymptomResponse>`
   - `POST /infer/medical` 호출 (`query`, `max_length=64`, `temperature=0.1`)
   - `LlmResponse.generatedText` 정규식 파싱 유지, 실패 시 `LlmServiceUnavailableException` throw
2. `SymptomController` 수정 — 반환 타입 `Mono<SymptomResponse>`로 변경
3. `SymptomControllerTest` 수정 — `Mono.just()` mock + `asyncDispatch` 2단계 패턴 적용
4. `./gradlew test` — 전체 통과 확인

---

## 완료 기준

- [ ] SymptomAnalysisService — llmWebClient 사용, Mono<SymptomResponse> 반환
- [ ] SymptomController — Mono<SymptomResponse> 반환
- [ ] SymptomControllerTest — 수정 후 GREEN
- [ ] `./gradlew test` 전체 통과
