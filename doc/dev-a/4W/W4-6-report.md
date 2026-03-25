# W4-6 리포트 - Controller 및 DTO 이식

## 작업 개요
- **작업명**: spring-python-llm-exam-mng Controller/DTO를 HMS `llm/` 패키지로 이식, API 경로 및 Security 정책 반영
- **수정 파일**:
  - `domain/ChatbotHistoryRepository.java` — Page 버전 메서드 추가
  - `domain/MedicalHistoryRepository.java` — Page 버전 메서드 추가
  - `config/SecurityConfig.java` — LLM 경로 접근 제어 + CSRF ignore 추가
- **신규 DTO** (`llm/dto/`):
  - `ChatbotHistoryResponse.java`, `MedicalLlmResponse.java`, `MedicalHistoryResponse.java`
- **신규 Controller** (`llm/controller/`):
  - `ChatController.java`, `MedicalController.java`, `LlmReservationController.java`

## 작업 내용

### 1. Repository Pageable 메서드 추가
히스토리 조회 엔드포인트에서 `Page<T>` 반환을 위해 두 Repository에 Pageable 버전 메서드 추가.

```java
// ChatbotHistoryRepository
Page<ChatbotHistory> findByStaff_IdOrderByCreatedAtDesc(Long staffId, Pageable pageable);

// MedicalHistoryRepository
Page<MedicalHistory> findByStaff_IdOrderByCreatedAtDesc(Long staffId, Pageable pageable);
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 특정 직원의 챗봇/의료 상담 이력을 페이지 단위로 조회하는 메서드를 추가합니다. `Page<T>`는 전체 결과를 한 번에 가져오지 않고 "1페이지", "2페이지" 형태로 나눠서 가져올 수 있는 결과 타입입니다.
> - **왜 이렇게 썼는지**: 대화 이력이 수백, 수천 건이 될 수 있는데 전부 한 번에 가져오면 서버에 부담이 큽니다. `Pageable` 파라미터를 추가하면 `?page=0&size=10` 형태로 요청해서 10건씩 나눠 받을 수 있습니다.
> - **쉽게 말하면**: 도서관 목록을 한 번에 전부 보여주는 대신, 10권씩 페이지를 나눠서 보여주는 것과 같습니다.

### 2. DTO 추가
| DTO | 원본 | HMS 변경 사항 |
|---|---|---|
| `ChatbotHistoryResponse` | `ChatHistoryResponse` (ChatHistory 기반) | `ChatbotHistory` 기반으로 재작성 |
| `MedicalLlmResponse` | 동일 | 패키지명만 변경 |
| `MedicalHistoryResponse` | 동일 | 패키지명만 변경 |

### 3. ChatController 이식

| 항목 | spring-llm | HMS |
|---|---|---|
| 경로 | `/api/chat` | `/llm/chatbot` |
| staffId 획득 | `@RequestHeader("X-Staff-Id")` 헤더 | Security principal → `resolveStaffId()` |
| History Repository | `ChatHistoryRepository` | `ChatbotHistoryRepository` |
| History Response | `ChatHistoryResponse` | `ChatbotHistoryResponse` |
| Stream 엔드포인트 | `/query/stream` | 유지 |

`/query` 엔드포인트: staffId가 null인 경우(비인증) 히스토리 저장 스킵.

### 4. MedicalController 이식

| 항목 | spring-llm | HMS |
|---|---|---|
| 경로 | `/api/medical` | `/llm/medical` |
| userId 획득 | `@RequestHeader("X-User-Id")` 헤더 | Security principal → `resolveStaffId()` |
| `/query` | `callLlmApi()` 호출 | `callMedicalLlmApi()`로 통합 (Task 5에서 중복 제거) |
| `/medical-query` | 별도 엔드포인트 | 제거 (callMedicalLlmApi 통합) |

### 5. LlmReservationController (신규)
spring-llm `ReservationApiController`의 예약 생성(`POST`)은 제외하고,
**가용 슬롯 조회(`GET /slots/{doctorId}`)만** 이식.
- `LlmReservationService.getAvailableSlots()` 위임

### 6. SecurityConfig 변경

#### CSRF ignore 추가
```java
.ignoringRequestMatchers(
    "/llm/symptom/**",
    "/llm/medical/**",
    "/llm/chatbot/**",
    "/llm/reservation/**")
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 지정된 LLM 관련 경로들에 대해 CSRF(사이트 간 요청 위조) 토큰 검사를 생략하도록 설정합니다.
> - **왜 이렇게 썼는지**: CSRF 보호는 주로 브라우저 폼 제출에서 악의적인 사이트가 사용자 권한으로 요청을 위조하는 것을 방지합니다. LLM 경로는 JavaScript `fetch`로 JSON을 주고받는 API 방식이며, CSRF 토큰 없이 호출하기 때문에 ignore 처리해야 요청이 차단되지 않습니다.
> - **쉽게 말하면**: 도장(CSRF 토큰) 없이도 통과할 수 있는 문을 지정하는 것으로, LLM API 문들은 도장 검사를 하지 않겠다고 설정합니다.

#### 접근 제어 추가
| 경로 | 정책 |
|---|---|
| `/llm/medical/**` | `permitAll` |
| `/llm/reservation/**` | `permitAll` |
| `/llm/chatbot/**` | `authenticated` |

## 빌드 결과
```
BUILD SUCCESSFUL in 2s
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: Controller, DTO, SecurityConfig 수정 후 전체 프로젝트가 오류 없이 빌드되었음을 나타냅니다.
> - **왜 이렇게 썼는지**: SecurityConfig 수정은 잘못될 경우 기존 인증 흐름 전체에 영향을 줄 수 있으므로 빌드 확인이 중요합니다.
> - **쉽게 말하면**: 건물 보안 시스템을 수정한 후 기존 출입증들이 여전히 잘 동작하는지 확인하는 과정입니다.

(기존 SecurityConfig의 `permissionsPolicy` unchecked 경고는 Spring Boot 4.0.3 API 변경 관련 기존 경고로 이번 작업과 무관)

## 특이사항
- `resolveStaffId()` 헬퍼 메서드를 `ChatController`, `MedicalController` 양쪽에 동일하게 작성 — 추후 공통 유틸로 추출 가능
- spring-llm `ReservationApiController`의 `POST /api/reservation`(예약 생성)은 이식 제외 — HMS 기존 `ReservationService`와 충돌 가능성 있어 Task 6 범위 밖
- `/llm/chatbot/**`는 `authenticated` 정책이지만 CSRF ignore 처리하여 JS fetch 호출 시 CSRF 토큰 불필요
