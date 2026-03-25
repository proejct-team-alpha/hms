# W4-6 Workflow — Controller 및 DTO 이식

> **작성일**: 4W
> **브랜치**: `feature/Llm`
> **목표**: spring-python-llm Controller/DTO를 HMS `llm/` 패키지로 이식 + SecurityConfig 조정

---

## 전체 흐름

```
Repository Pageable 메서드 추가 → DTO 신규 생성
  → ChatController, MedicalController 이식
  → LlmReservationController 신규
  → SecurityConfig LLM 경로 접근 제어 + CSRF ignore 추가
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| API 경로 | `/api/chat/**` → `/llm/chatbot/**`, `/api/medical/**` → `/llm/medical/**` |
| 인증 정책 | `/llm/medical/**` permitAll, `/llm/chatbot/**` authenticated |
| CSRF | LLM 경로 ignore 추가 |
| LlmReservationController | GET /llm/reservation/slots/{doctorId}, 비회원 허용 |

---

## 실행 흐름

```
[1] ChatbotHistoryRepository, MedicalHistoryRepository — Page 버전 메서드 추가
[2] ChatbotHistoryResponse, MedicalLlmResponse, MedicalHistoryResponse DTO 신규
[3] ChatController 이식 — @RequestMapping("/llm/chatbot")
[4] MedicalController 이식 — @RequestMapping("/llm/medical")
[5] LlmReservationController 신규 — GET /llm/reservation/slots/{doctorId}
[6] SecurityConfig 수정 — CSRF ignore + 접근 제어 추가
[7] ./gradlew build 검증
```

---

## UI Mockup

```
[Controller 이식 작업 — UI 없음]

API 경로 매핑:
POST /api/chat/query          → POST /llm/chatbot/query
GET  /api/chat/history/{id}   → GET  /llm/chatbot/history/{id}
POST /api/medical/query       → POST /llm/medical/query
GET  /api/medical/history/{id}→ GET  /llm/medical/history/{id}
GET  /api/reservation/slots/  → GET  /llm/reservation/slots/{doctorId}
```

---

## 작업 목록

1. `ChatbotHistoryRepository` — `Page<ChatbotHistory> findByStaff_IdOrderByCreatedAtDesc` 추가
2. `MedicalHistoryRepository` — `Page<MedicalHistory> findByStaff_IdOrderByCreatedAtDesc` 추가
3. `ChatbotHistoryResponse`, `MedicalLlmResponse`, `MedicalHistoryResponse` DTO 신규
4. `ChatController.java` 이식 — `/llm/chatbot`, Security principal 패턴
5. `MedicalController.java` 이식 — `/llm/medical`, Security principal 패턴
6. `LlmReservationController.java` 신규 — `GET /llm/reservation/slots/{doctorId}`
7. `SecurityConfig.java` 수정 — CSRF ignore + 접근 제어 추가
8. `./gradlew build` 검증

---

## 작업 진행내용

- [x] Repository Pageable 메서드 추가
- [x] 추가 DTO 생성
- [x] ChatController 이식
- [x] MedicalController 이식
- [x] LlmReservationController 신규
- [x] SecurityConfig 수정
- [x] 빌드 확인 — BUILD SUCCESSFUL

---

## 실행 흐름에 대한 코드

### SecurityConfig 변경

```java
// CSRF ignore 추가
.ignoringRequestMatchers("/llm/symptom/**", "/llm/medical/**", "/llm/chatbot/**", "/llm/reservation/**")

// 접근 제어 추가
.requestMatchers("/llm/medical/**", "/llm/reservation/**").permitAll()
.requestMatchers("/llm/chatbot/**").authenticated()
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 두 가지 보안 설정을 추가합니다. 첫 번째(`ignoringRequestMatchers`)는 LLM 관련 경로에서 CSRF 토큰 검사를 생략합니다. 두 번째는 경로별 접근 권한을 설정하는데, `/llm/medical/**`과 `/llm/reservation/**`은 누구나 접근 가능(`permitAll`)하고, `/llm/chatbot/**`은 로그인한 사용자만 접근 가능(`authenticated`)하도록 합니다.
> - **왜 이렇게 썼는지**: CSRF(사이트 간 요청 위조) 보호는 브라우저 폼 기반 요청에 필요한데, LLM 경로는 JavaScript `fetch`로 JSON을 주고받는 API 방식이므로 CSRF 토큰이 필요 없습니다. 접근 제어는 AI 증상 상담(`/llm/medical`)은 비로그인 환자도 쓸 수 있고, 병원 내부 챗봇(`/llm/chatbot`)은 직원만 쓸 수 있도록 구분합니다.
> - **쉽게 말하면**: 정문은 누구나 통과(`permitAll`), 직원실은 사원증 필요(`authenticated`)처럼 문마다 다른 잠금장치를 설정하는 것입니다.

### Controller — Security principal 추출 패턴

```java
private Long resolveStaffId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
        return null;
    }
    return staffRepository.findByUsernameAndActiveTrue(auth.getName())
            .map(Staff::getId).orElse(null);
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 현재 HTTP 요청을 보낸 사람이 로그인한 직원이면 그 직원의 ID를 반환하고, 비로그인 상태면 `null`을 반환하는 헬퍼 메서드입니다.
> - **왜 이렇게 썼는지**: `SecurityContextHolder`에서 인증 정보를 꺼내는 과정에서 비로그인(익명) 사용자인 경우를 세 단계로 방어합니다. `auth == null`, `!auth.isAuthenticated()`, `"anonymousUser"` 체크가 각각 다른 비로그인 상황을 커버합니다. 마지막에 `findByUsernameAndActiveTrue()`로 실제 DB에서 활성 직원을 확인해 ID를 가져옵니다.
> - **쉽게 말하면**: "지금 요청한 사람이 누군지 확인하는 신분증 조회기"로, 손님이면 null, 직원이면 직원 번호를 돌려줍니다.

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| 빌드 | ./gradlew build | BUILD SUCCESSFUL |
| API 경로 | 컨트롤러 Bean 매핑 확인 | 충돌 없음 |
| /llm/medical 접근 | 비인증 | 200 OK |
| /llm/chatbot 접근 | 비인증 | 3xx 리다이렉트 |

---

## 완료 기준

- [x] `./gradlew build` 오류 없음
- [x] 컨트롤러 Bean 매핑 충돌 없음
- [x] API 경로 중복 없음
