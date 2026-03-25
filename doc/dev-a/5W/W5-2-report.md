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

---

> **💡 입문자 설명**
>
> **컨트롤러 테스트란 무엇인지**
> - 컨트롤러 테스트는 실제 서버를 켜지 않고 "이 URL로 요청을 보내면 올바른 응답이 오는지"를 확인합니다. MockMvc라는 도구를 사용해 가상의 HTTP 요청을 보내고 응답을 검증합니다.
> - 서비스나 DB는 가짜(Mock) 객체로 대체하므로, 컨트롤러 로직만 격리해서 테스트할 수 있습니다.
>
> **비인증 리다이렉트 테스트 — 왜 필요한지**
> - "로그인하지 않은 사용자가 이 URL에 접근하면 로그인 페이지로 보내야 한다"는 보안 규칙을 테스트합니다. 이 테스트가 없으면 나중에 보안 설정이 실수로 바뀌어도 아무도 모릅니다.
>
> **`Mono<T>` 비동기 테스트 패턴 — 왜 복잡한지**
> - `Mono<T>`는 WebFlux의 비동기 데이터 타입입니다. 일반 동기 컨트롤러는 즉시 응답을 반환하지만, `Mono<T>`는 "나중에 값이 생성될 것"을 약속하는 객체입니다.
> - MockMvc는 기본적으로 동기 방식이라, 비동기 응답을 테스트하려면 `asyncStarted()` 확인 → `asyncDispatch(result)` 재처리 두 단계가 필요합니다. "비동기 처리가 시작됐는지 확인하고, 완료된 결과를 다시 검증하는" 과정입니다.
> - **다른 방법**: WebTestClient를 쓰면 비동기를 더 자연스럽게 테스트할 수 있지만, 이 프로젝트는 MockMvc를 표준으로 사용합니다.
>
> **Mock 객체 — 왜 쓰는지**
> - 테스트에서 실제 DB를 사용하면 느리고, 테스트 데이터가 남아 문제가 됩니다. Mock은 "실제처럼 동작하는 가짜 객체"로, `willReturn()`으로 미리 반환값을 지정합니다.
> - `resolveStaffId()` mock에서 `Optional.empty()`를 반환하는 이유: 직원을 찾지 못해도 로직이 graceful하게 처리되는지 검증하기 위해서입니다.
>
> **쉽게 말하면**: "실제 병원을 운영하지 않고 모형 병원을 만들어서 각 창구가 올바르게 작동하는지 연습해보는 것"과 같습니다.
