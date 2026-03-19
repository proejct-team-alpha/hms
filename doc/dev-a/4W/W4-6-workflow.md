# W4-6 Controller 및 DTO 이식

## 작업 목표
spring-python-llm-exam-mng의 Controller와 DTO를 HMS `llm/` 패키지로 이식하고
API 경로 및 SecurityConfig를 HMS 정책에 맞게 조정한다.

## 작업 목록
<!-- TODO 1. Repository Pageable 메서드 추가 (ChatbotHistoryRepository, MedicalHistoryRepository) -->
<!-- TODO 2. 추가 DTO 생성 (ChatbotHistoryResponse, MedicalLlmResponse, MedicalHistoryResponse) -->
<!-- TODO 3. ChatController 이식 (/llm/chatbot, Security principal) -->
<!-- TODO 4. MedicalController 이식 (/llm/medical, Security principal) -->
<!-- TODO 5. LlmReservationController 신규 (/llm/reservation) -->
<!-- TODO 6. SecurityConfig — LLM 경로 접근 제어 + CSRF 추가 -->
<!-- TODO 7. 빌드 확인 -->

## 진행 현황
- [x] 1. Repository Pageable 메서드 추가
- [x] 2. 추가 DTO 생성
- [x] 3. ChatController 이식
- [x] 4. MedicalController 이식
- [x] 5. LlmReservationController 신규
- [x] 6. SecurityConfig 수정
- [x] 7. 빌드 확인 — BUILD SUCCESSFUL

## 수정/추가 파일
**수정**
- `domain/ChatbotHistoryRepository.java` — Page 버전 메서드 추가
- `domain/MedicalHistoryRepository.java` — Page 버전 메서드 추가
- `config/SecurityConfig.java` — LLM 경로 접근 제어 + CSRF ignore 추가

**신규 DTO**
- `llm/dto/ChatbotHistoryResponse.java`
- `llm/dto/MedicalLlmResponse.java`
- `llm/dto/MedicalHistoryResponse.java`

**신규 Controller**
- `llm/controller/ChatController.java`
- `llm/controller/MedicalController.java`
- `llm/controller/LlmReservationController.java`

---

## 상세 내용

### API 경로 매핑
| spring-llm | HMS |
|---|---|
| `POST /api/chat/query` | `POST /llm/chatbot/query` |
| `POST /api/chat/query/stream` | `POST /llm/chatbot/query/stream` |
| `GET /api/chat/history/{staffId}` | `GET /llm/chatbot/history/{staffId}` |
| `POST /api/medical/query` | `POST /llm/medical/query` |
| `POST /api/medical/medical-query` | (callLlmApi → callMedicalLlmApi 통합으로 제거) |
| `POST /api/medical/query/consult` | `POST /llm/medical/query/consult` |
| `POST /api/medical/query/stream` | `POST /llm/medical/query/stream` |
| `GET /api/medical/history/{staffId}` | `GET /llm/medical/history/{staffId}` |
| `GET /api/reservation/slots/{doctorId}` | `GET /llm/reservation/slots/{doctorId}` |

### Security principal 추출 패턴
```java
// Controller에서 staffId 추출
private Long resolveStaffId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
        return null;
    }
    return staffRepository.findByUsernameAndActiveTrue(auth.getName())
            .map(Staff::getId).orElse(null);
}
```

### SecurityConfig 변경 사항
```
// CSRF ignore 추가
.ignoringRequestMatchers("/llm/symptom/**", "/llm/medical/**", "/llm/chatbot/**", "/llm/reservation/**")

// 접근 제어 추가 (기존 /llm/symptom/** 위에)
.requestMatchers("/llm/medical/**", "/llm/reservation/**").permitAll()
.requestMatchers("/llm/chatbot/**").authenticated()
```

## 수용 기준
- [ ] `./gradlew build` 오류 없음
- [ ] 컨트롤러 Bean 매핑 충돌 없음
- [ ] API 경로 중복 없음
