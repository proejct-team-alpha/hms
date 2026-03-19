# W5-2 Report — ChatControllerTest / MedicalControllerTest / SymptomControllerTest

> **작성일**: 2026-03-19
> **브랜치**: `feature/reservation-Llm`
> **빌드**: BUILD SUCCESSFUL

---

## 작업 완료 목록

| # | 파일 | 테스트 수 | 상태 |
|---|------|-----------|------|
| 1 | `ChatControllerTest.java` | 4개 | ✅ |
| 2 | `MedicalControllerTest.java` | 3개 | ✅ |
| 3 | `SymptomControllerTest.java` | 2개 | ✅ |
| 4 | `./gradlew test` 전체 통과 | 32 클래스 | ✅ |

---

## LLM 테스트 클래스 현황 (5종 완비)

| 클래스 | 테스트 |
|--------|--------|
| `LlmPageControllerTest` | 3개 (기존) |
| `LlmReservationControllerTest` | 2개 (기존) |
| `ChatControllerTest` | 4개 (신규) |
| `MedicalControllerTest` | 3개 (신규) |
| `SymptomControllerTest` | 2개 (신규) |

---

## 테스트 케이스 목록

### ChatControllerTest (4개)

| 테스트 | 결과 |
|--------|------|
| POST /llm/chatbot/query — 비인증 리다이렉트 | PASS |
| POST /llm/chatbot/query — DOCTOR 인증 200 | PASS |
| POST /llm/chatbot/query/stream — DOCTOR 인증 SSE 200 | PASS |
| GET /llm/chatbot/history/{staffId} — DOCTOR 인증 200 | PASS |

### MedicalControllerTest (3개)

| 테스트 | 결과 |
|--------|------|
| POST /llm/medical/query — 비인증 200 (permitAll) | PASS |
| POST /llm/medical/query/consult — 비인증 200, recommendedDepartment 검증 | PASS |
| GET /llm/medical/history/{staffId} — 인증 200 | PASS |

### SymptomControllerTest (2개)

| 테스트 | 결과 |
|--------|------|
| POST /llm/symptom/analyze — 비인증 200 (permitAll) | PASS |
| 응답 구조 확인 — dept, doctor, time 필드 | PASS |

---

## 비고

- `Mono<T>` 반환 컨트롤러: `andExpect(request().asyncStarted()).andReturn()` + `asyncDispatch(result)` 패턴 적용
- `MedicalService.saveMedicalPending()` mock: `new MedicalHistory(question, "PENDING")` 팩토리 사용
- `resolveStaffId()` mock: `staffRepository.findByUsernameAndActiveTrue(any()).willReturn(Optional.empty())`
