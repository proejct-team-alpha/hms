# W5-2 Workflow — ChatControllerTest / MedicalControllerTest / SymptomControllerTest

> **작성일**: 2026-03-19
> **브랜치**: `feature/reservation-Llm`
> **목표**: W4-8에서 누락된 테스트 2개 + W5-1 신규 컨트롤러 테스트 1개 작성

---

## 작업 목록

```
// [1] ChatControllerTest 신규
//     MockBean: ChatService, ChatbotHistoryRepository, StaffRepository
//     - POST /llm/chatbot/query — 비인증 리다이렉트
//     - POST /llm/chatbot/query — DOCTOR 인증 200 (Mono<String> asyncDispatch)
//     - POST /llm/chatbot/query/stream — DOCTOR 인증 200 (Flux SSE)
//     - GET /llm/chatbot/history/{staffId} — DOCTOR 인증 200
//
// [2] MedicalControllerTest 신규
//     MockBean: MedicalService, MedicalHistoryRepository, DoctorService, LlmResponseParser, StaffRepository
//     - POST /llm/medical/query — 비인증 200 (permitAll, Mono<String> asyncDispatch)
//     - POST /llm/medical/query/consult — 비인증 200 (Mono<MedicalLlmResponse> asyncDispatch)
//     - GET /llm/medical/history/{staffId} — 인증 200
//
// [3] SymptomControllerTest 신규
//     MockBean: SymptomAnalysisService
//     - POST /llm/symptom/analyze — 비인증 200 (permitAll)
//     - 응답 구조 확인 — dept, doctor, time 필드
//
// [4] ./gradlew test 전체 통과 확인
```

---

## 테스트 패턴 (기존 준수)

- `@WebMvcTest(대상.class)` + `@Import(TestClass.TestSecurityConfig.class)`
- 내부 `@TestConfiguration @EnableWebSecurity static class TestSecurityConfig`
- `@MockitoBean` — 서비스/레포지토리 mock
- `Mono<T>` 비동기 반환: `andReturn()` + `asyncDispatch(result)` 2단계

## 완료 기준

- [ ] ChatControllerTest 4개 GREEN
- [ ] MedicalControllerTest 3개 GREEN
- [ ] SymptomControllerTest 2개 GREEN
- [ ] `./gradlew test` 전체 통과
