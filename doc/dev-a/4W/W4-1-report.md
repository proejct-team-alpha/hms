# W4-1 리포트 — HMS 패키지 구조 확인 및 Entity 스키마 비교

## 작업 개요
- **날짜**: 2026-03-19
- **브랜치**: `feature/Llm`
- **목표**: HMS 패키지 구조 파악 + 중복 Entity 스키마 비교 + 통합 전략 확정

---

## 1. HMS 패키지 구조 확인

### 확인된 패키지 목록

```
com.smartclinic.hms
├── config/          SecurityConfig, WebMvcConfig, ClaudeApiConfig, RateLimitFilter
├── common/          exception/, interceptor/, util/
├── domain/          Entity + Enum 클래스
├── admin/           dashboard, department, item, mypage, reservation, rule, staff
├── staff/           dashboard, dto, item, mypage, reception, reservation, walkin
├── doctor/          chatbot, mypage, treatment
├── nurse/           mypage + NurseService 등
├── reservation/     reservation/
├── llm/             ★ Java 파일 없음 (FILES.md, AI-CONTEXT.md만 존재)
├── auth/            AuthController, CustomUserDetailsService, StaffRepository
├── home/            HomeController
└── item/            ItemManagerController 등
```

**주요 발견**: `llm/` 패키지는 존재하지만 실제 Java 파일이 없는 빈 상태.
FILES.md에 LlmController, LlmService, ChatbotController, LlmRecommendationRepository가 명시되어 있으나 미구현.

---

## 2. HMS domain Entity 목록

| Entity | 테이블 | 주요 특이사항 |
|--------|--------|--------------|
| `ChatbotHistory` | chatbot_history | sessionId, staff(FK), question, answer, createdAt |
| `HospitalRule` | hospital_rule | category(enum HospitalRuleCategory), is_active |
| `Reservation` | reservation | timeSlot(String), ReservationStatus(enum), ReservationSource(enum) |
| `Doctor` | doctor | staff(OneToOne FK), availableDays(String), Department(FK) |
| `Staff` | staff | StaffRole(enum), employeeNumber, is_active |
| `Department` | department | - |
| `Patient` | patient | - |
| `LlmRecommendation` | llm_recommendation | - |
| `TreatmentRecord` | treatment_record | - |
| `Item` | item | ItemCategory(enum) |

---

## 3. 중복 Entity 비교 및 통합 전략

### 3.1 ChatHistory vs ChatbotHistory

| 컬럼 | spring_llm (ChatHistory) | HMS (ChatbotHistory) | 전략 |
|------|--------------------------|----------------------|------|
| id | Long | Long | HMS 기준 |
| staff_id | user_id (Long, FK→Staff) | staff_id (FK→Staff) | HMS 기준 |
| session_id | O (length 100) | O (length 100) | 동일 |
| question | query | question | HMS 기준 |
| answer | response | answer | HMS 기준 |
| status | O (PENDING/COMPLETED/FAILED) | **없음** | **ChatbotHistory에 추가 필요** |
| metadata | O (TEXT/JSON) | **없음** | **ChatbotHistory에 추가 필요** |
| created_at | createdAt | createdAt | HMS 기준 |

**결정**: HMS `ChatbotHistory` 사용. `status`, `metadata` 컬럼 추가 필요 (Task 4에서 처리).

---

### 3.2 MedicalRule vs HospitalRule

| 컬럼 | spring_llm (MedicalRule) | HMS (HospitalRule) | 전략 |
|------|--------------------------|---------------------|------|
| category | String (50) | enum HospitalRuleCategory | HMS 기준 (enum 사용) |
| title | O | O | 동일 |
| content | LONGTEXT | TEXT | HMS 기준 |
| is_active | **없음** | O | HMS 기준 유지 |
| target | O (length 100) | **없음** | **추가 불필요** |
| start_date | O | **없음** | **추가 불필요** |
| end_date | O | **없음** | **추가 불필요** |
| created_at | O | O | HMS 기준 |
| updated_at | **없음** | O | HMS 기준 유지 |

**결정**: HMS `HospitalRule` 기준 사용. Python RAG는 hospital_rule 테이블 데이터를 그대로 활용.
`target`, `start_date`, `end_date` 추가 불필요 — is_active로 활성/비활성 관리.

---

### 3.3 Reservation 통합

| 컬럼 | spring_llm (reservation_tb) | HMS (reservation) | 전략 |
|------|------------------------------|-------------------|------|
| reservationNumber | **없음** | O (unique) | HMS 기준 |
| patient | **없음** | patient(FK) | HMS 기준 |
| doctor | doctor_id(FK) | doctor(FK) | HMS 기준 |
| department | **없음** | department(FK) | HMS 기준 |
| reservation_date | O | O | HMS 기준 |
| start_time / end_time | O (LocalTime) | **없음** | HMS의 `timeSlot`으로 대체 |
| timeSlot | **없음** | O (String, 예: "09:00") | HMS 기준 사용 |
| status | String | enum ReservationStatus | HMS 기준 (enum 사용) |
| source | **없음** | enum ReservationSource | HMS 기준 |

**결정**: HMS `Reservation` 기준 사용. spring_llm의 start_time/end_time → HMS timeSlot으로 대체.
spring_llm의 DoctorSchedule 기반 슬롯 조회 로직은 HMS 방식에 맞게 조정.

---

### 3.4 Doctor / Staff / DoctorSchedule

| Entity | 전략 | 비고 |
|--------|------|------|
| `Doctor` | HMS 것 사용 | staff(OneToOne), availableDays 이미 존재 |
| `Staff` | HMS 것 사용 | StaffRole enum, employeeNumber 포함 |
| `DoctorSchedule` | **추가 불필요** | HMS는 Doctor.availableDays + Reservation.timeSlot 방식으로 관리 |

---

## 4. LLM 전용 신규 Entity (Task 4에서 domain에 추가 예정)

| Entity | 테이블 | 주요 컬럼 |
|--------|--------|-----------|
| `MedicalHistory` | medical_history | staff(FK), sessionId, question, answer, status, metadata, createdAt |
| `MedicalContent` | medical_content | cId, domain(Integer), content(LONGTEXT), dataset, dataType, language |
| `MedicalQa` | medical_qa | qaId, domain(Integer), department, qType, question, answer, dataset |
| `MedicalDomain` | medical_domain | domainId(Integer PK), domainName |

---

## 5. HMS llm 패키지 현황

**현재 상태**: Java 파일 없음 (빈 패키지)

**FILES.md 기준 구현 예정 파일**:

| 파일 | 설명 | 비고 |
|------|------|------|
| `LlmController.java` | POST /llm/symptom/analyze | Claude API 기반 |
| `LlmService.java` | Claude API 호출 | 수정 금지 영역 |
| `ChatbotController.java` | POST /llm/chatbot/ask | 직원용 챗봇 |
| `LlmRecommendationRepository.java` | LLM 추천 이력 저장 | |

**병합 후 추가할 파일** (spring_llm에서 이식):
- `llm/controller/ChatController.java` — 병원규칙 Q&A + SSE 스트리밍
- `llm/controller/MedicalController.java` — 의료 상담 + SSE 스트리밍
- `llm/service/ChatService.java` — Python WebClient 호출
- `llm/service/MedicalService.java` — Python WebClient 호출

---

## 수정 파일 목록

없음 (조사/분석 작업)

---

## 다음 작업

→ `W4-2-workflow.md` (Task 2: build.gradle 의존성 및 설정 파일 병합)
