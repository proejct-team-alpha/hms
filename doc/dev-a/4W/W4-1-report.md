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

> **💡 입문자 설명**
> - **Entity 통합이란**: 두 프로젝트에 같은 역할의 테이블이 따로 존재할 때, 하나를 기준으로 합치는 작업입니다. 코드 중복을 없애고 DB 구조를 단순하게 만듭니다.
> - **HMS 기준을 선택한 이유**: HMS가 이미 운영 중인 메인 프로젝트이므로 기존 컬럼명과 데이터를 유지해야 합니다. spring-llm의 `user_id`, `query`, `response` 같은 이름 대신 HMS의 `staff_id`, `question`, `answer`를 그대로 사용합니다.
> - **`status`, `metadata` 추가가 필요한 이유**: spring-llm에서는 챗봇 요청이 처리 중(PENDING)인지, 완료(COMPLETED)인지, 실패(FAILED)인지 추적합니다. HMS `ChatbotHistory`에는 이 정보가 없어 추가가 필요합니다.
> - **쉽게 말하면**: 두 팀이 각자 만든 "대화 기록 노트"를 하나로 합치되, 메인 팀(HMS) 형식을 기준으로 상대방 팀의 유용한 항목만 추가합니다.

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

> **💡 입문자 설명**
> - **enum vs String**: spring-llm은 `category`를 `String(50)`(단순 문자열)으로 저장하고, HMS는 Java `enum`(미리 정해진 값 목록)을 씁니다. enum은 "내과", "외과" 같은 값을 코드에서 타입 안전하게 관리할 수 있고, 오타나 잘못된 값이 컴파일 단계에서 잡힙니다.
> - **`target`, `start_date`, `end_date` 추가하지 않는 이유**: spring-llm은 규칙의 적용 대상과 기간을 별도 컬럼으로 관리했지만, HMS는 `is_active`(활성/비활성 플래그) 하나로 단순화했습니다. 프로젝트 요구사항에 기간별 규칙 관리가 없으므로 복잡성을 늘릴 필요가 없습니다.
> - **쉽게 말하면**: 병원 규칙을 관리하는 "규칙집"을 통합할 때, 더 단순하고 이미 잘 동작하는 HMS 방식을 유지하고, 불필요한 기능은 추가하지 않는 결정입니다.

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

> **💡 입문자 설명**
> - **`start_time/end_time` vs `timeSlot`**: spring-llm은 예약 시간을 시작·종료 시각 두 개로 저장했습니다. HMS는 "09:00" 같은 고정 슬롯 문자열 하나로 관리합니다. HMS 방식은 단순하고 슬롯 기반 예약(정해진 시간대 중 선택)에 적합합니다.
> - **`reservationNumber`**: HMS에만 있는 고유 예약 번호 필드입니다. "RES-20260319-001" 형태로 환자가 예약을 조회할 때 사용합니다. spring-llm에는 없었지만 HMS의 핵심 기능이므로 유지합니다.
> - **`enum` 상태 관리**: spring-llm은 상태를 String으로 저장했지만, HMS는 `ReservationStatus` enum을 씁니다. 가능한 상태값이 코드에 명시되므로 잘못된 상태가 들어오는 실수를 방지합니다.
> - **쉽게 말하면**: "예약 시스템의 중심 테이블"을 통합할 때, 예약 번호·환자 정보·부서 정보 등 HMS만의 기능을 살리고, spring-llm의 시작/종료 시각 방식은 HMS 슬롯 방식으로 대체합니다.

---

### 3.4 Doctor / Staff / DoctorSchedule

| Entity | 전략 | 비고 |
|--------|------|------|
| `Doctor` | HMS 것 사용 | staff(OneToOne), availableDays 이미 존재 |
| `Staff` | HMS 것 사용 | StaffRole enum, employeeNumber 포함 |
| `DoctorSchedule` | **추가 불필요** | HMS는 Doctor.availableDays + Reservation.timeSlot 방식으로 관리 |

> **💡 입문자 설명**
> - **DoctorSchedule이 불필요한 이유**: spring-llm은 의사 스케줄을 별도 테이블로 정교하게 관리했습니다. HMS는 `Doctor.availableDays`(예: "월,화,수")와 `Reservation.timeSlot`(예: "09:00")의 조합으로 더 단순하게 구현합니다. 두 방식 모두 같은 기능을 달성하지만, HMS 방식이 기존 코드와의 호환성이 높아 이를 유지합니다.
> - **쉽게 말하면**: 복잡한 스케줄 관리 테이블 대신, 의사 정보에 요일을 직접 저장하는 단순한 방식을 선택한 것입니다.

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
