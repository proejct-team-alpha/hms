# W4-8 리포트 - 통합 테스트 및 최종 검증

## 작업 개요
- **작업명**: LLM 병합(Task 2~7) 결과물에 대한 JUnit 슬라이스 테스트 작성, 전체 테스트 통과 확인,
  LLM_SERVICE_URL 환경변수 설정, 수동 시나리오 체크리스트 정의
- **신규 테스트 파일**:
  - `test/.../llm/LlmPageControllerTest.java`
  - `test/.../llm/ChatControllerTest.java`
  - `test/.../llm/MedicalControllerTest.java`
  - `test/.../llm/LlmReservationControllerTest.java`
- **수정 파일**:
  - `.env.example` — `LLM_SERVICE_URL` 항목 추가

## 작업 내용

### 1. LLM 슬라이스 테스트 (4종)

공통 패턴: `@WebMvcTest` + inner `@TestConfiguration TestSecurityConfig` + `@MockitoBean`

#### LlmPageControllerTest (3개)

| 테스트 | 결과 |
|---|---|
| GET /llm/medical — 비인증 200 (permitAll) | PASS |
| GET /llm/chatbot — 비인증 /login 리다이렉트 | PASS |
| GET /llm/chatbot — DOCTOR 인증 200 | PASS |

#### ChatControllerTest (4개)

| 테스트 | 결과 |
|---|---|
| POST /llm/chatbot/query — 비인증 리다이렉트 | PASS |
| POST /llm/chatbot/query — DOCTOR 인증 200 | PASS |
| POST /llm/chatbot/query/stream — DOCTOR 인증 SSE 200 | PASS |
| GET /llm/chatbot/history/{staffId} — DOCTOR 인증 Page 200 | PASS |

#### MedicalControllerTest (4개)

| 테스트 | 결과 |
|---|---|
| POST /llm/medical/query — 비인증 200 (permitAll) | PASS |
| POST /llm/medical/query/consult — 비인증 200, `recommendedDepartment` 검증 | PASS |
| POST /llm/medical/query/stream — 비인증 SSE 200 | PASS |
| GET /llm/medical/history/{staffId} — 인증 Page 200 | PASS |

#### LlmReservationControllerTest (2개)

| 테스트 | 결과 |
|---|---|
| GET /llm/reservation/slots/{doctorId} — 비인증 200 (permitAll) | PASS |
| 응답 구조 확인 — doctorId, doctorName, slots 필드 | PASS |

### 2. 전체 테스트 결과

```
150 tests — BUILD SUCCESSFUL
```

기존 테스트(137개) 회귀 없음. LLM 신규 테스트(13개) 전원 통과.

### 3. LLM_SERVICE_URL 환경변수

`.env.example`에 항목 추가:
```
LLM_SERVICE_URL=http://192.168.0.73:8000
```

`application-dev.properties` 기본값: `http://localhost:8000`
`.env` 파일에 실제 IP 설정 후 `.\run-dev.ps1` 실행.

### 4. 수동 시나리오 체크리스트

서버 기동: `.env` 설정 → `.\run-dev.ps1`

| 시나리오 | 방법 | 기대 결과 |
|---|---|---|
| AI 증상 상담 페이지 | 브라우저 GET /llm/medical (비인증) | 채팅 UI 렌더링 |
| 병원규칙 Q&A 미인증 | 브라우저 GET /llm/chatbot (비인증) | /login 리다이렉트 |
| 병원규칙 Q&A 인증 | DOCTOR 로그인 후 GET /llm/chatbot | 채팅 UI 렌더링 |
| 의료 상담 LLM 호출 | POST /llm/medical/query `{"query":"두통"}` | python-llm 응답 텍스트 |
| 의사 추천 API | POST /llm/medical/query/consult `{"query":"두통"}` | recommendedDepartment, doctors 포함 JSON |
| 슬롯 조회 | GET /llm/reservation/slots/1 | doctorId, doctorName, slots 포함 JSON |
| 챗봇 쿼리 | DOCTOR 세션 POST /llm/chatbot/query `{"query":"당직"}` | 응답 텍스트 반환 |

## 빌드 결과
```
BUILD SUCCESSFUL — 150 tests
```

## 특이사항
- `LlmPageControllerTest.chatbotPage_비인증_리다이렉트`: 최초 작성 시 `redirectedUrlPattern("**/login")`이
  `/login`과 불일치 → `redirectedUrl("/login")`으로 수정. Spring Security는 절대 경로로 리다이렉트.
- `MedicalControllerTest.consultQuery_비인증_200`: `Mono<MedicalLlmResponse>` 반환 컨트롤러는
  MockMvc에서 비동기 디스패치 처리 필요 → `request().asyncStarted()` + `asyncDispatch(mvcResult)` 패턴으로 수정.
- `ChatController.resolveStaffId()` mock 처리: `staffRepository.findByUsernameAndActiveTrue(any())`를
  `Optional.empty()` 반환으로 mock → staffId null 처리 경로 정상 동작 확인.
- LLM 슬라이스 테스트는 python-llm 서버 없이 mock으로 동작 — CI 환경에서도 통과 가능.
