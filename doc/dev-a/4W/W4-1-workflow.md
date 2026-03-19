# W4-1 Workflow — HMS 프로젝트 패키지 구조 확인 및 Entity 스키마 비교

> **작성일**: 2026-03-19
> **브랜치**: `feature/Llm`
> **기반 문서**: `spring-python-llm-exam-mng/doc/TASK_HMS_MERGE.md` Task 1
> **목표**: HMS 패키지 구조 파악 + 중복 Entity 스키마 비교 + 통합 전략 확정

---

## 작업 목록 (주석)

```
// [1] HMS 패키지 구조 전체 확인
//     - com.smartclinic.hms 하위 패키지 트리 파악
//     - config, common, domain, admin, staff, doctor, nurse, reservation, llm 존재 여부
//
// [2] HMS domain Entity 목록 확인
//     - ChatbotHistory, HospitalRule, Reservation, Doctor, Staff, DoctorSchedule 스키마 확인
//
// [3] spring_llm_sample_mng Entity 스키마 확인 (참조용)
//     - ChatHistory, MedicalRule, Reservation(reservation_tb) 컬럼 확인
//
// [4] 중복 Entity 비교 및 통합 전략 결정
//     - ChatHistory vs ChatbotHistory
//     - MedicalRule vs HospitalRule
//     - Reservation vs Reservation(reservation_tb)
//     - Doctor, Staff, DoctorSchedule 호환성
//
// [5] LLM 전용 신규 Entity 목록 확인
//     - MedicalHistory, MedicalContent, MedicalQa, MedicalDomain 존재 여부
//
// [6] HMS llm 패키지 현황 확인
//     - llm/ 하위 구조 및 기존 파일 파악
```

---

## Step 1: HMS 패키지 구조 확인

### 1.1 전체 패키지 트리

확인 대상:

```
com.smartclinic.hms
├── config/
├── common/
│   ├── interceptor/
│   ├── exception/
│   ├── util/
│   └── service/
├── domain/
├── admin/
├── staff/
├── doctor/
├── nurse/
├── reservation/
└── llm/
```

**확인 방법**: HMS `src/main/java/com/smartclinic/hms/` 디렉토리 트리 탐색

---

## Step 2: HMS domain Entity 스키마 확인

확인할 Entity:

| Entity | 테이블 | 확인 항목 |
|--------|--------|-----------|
| `ChatbotHistory` | chatbot_history | 컬럼, FK, 팩토리 메서드 |
| `HospitalRule` | hospital_rule | category 타입(enum), 컬럼 목록 |
| `Reservation` | reservation | 컬럼, FK, 테이블명 |
| `Doctor` | doctor | Department 연관, 컬럼 |
| `Staff` | staff | StaffRole enum, 컬럼 |
| `DoctorSchedule` | doctor_schedules | Doctor FK, 컬럼 |

---

## Step 3: spring_llm 참조 Entity 확인

확인 위치: `spring-python-llm-exam-mng/src/main/java/com/sample/llm/entity/`

| Entity | 테이블 | 주요 확인 컬럼 |
|--------|--------|---------------|
| `ChatHistory` | chatbot_history | user_id→staff_id, query→question, response→answer |
| `MedicalRule` | medical_rule | category(String), target, start_date, end_date |
| `Reservation` | reservation_tb | doctor_id, staff_id, start_time, end_time, status |

---

## Step 4: 중복 Entity 통합 전략 결정

### 4.1 ChatHistory vs ChatbotHistory

| 항목 | spring_llm | HMS | 전략 |
|------|-----------|-----|------|
| 테이블명 | chatbot_history | chatbot_history | **HMS `ChatbotHistory` 사용** |
| user_id | user_id (Long) | staff_id (FK→Staff) | HMS 기준 |
| query | query | question | HMS 기준 |
| response | response | answer | HMS 기준 |
| status | O (PENDING/COMPLETED/FAILED) | X | HMS에 status 추가 필요 여부 확인 |
| metadata | O (JSON) | X | HMS에 metadata 추가 필요 여부 확인 |

**결정**: HMS `ChatbotHistory` 사용. status/metadata 컬럼이 없으면 추가.

---

### 4.2 MedicalRule vs HospitalRule

| 항목 | spring_llm (medical_rule) | HMS (hospital_rule) | 전략 |
|------|--------------------------|---------------------|------|
| category | String | enum HospitalRuleCategory | **HMS `HospitalRule` 기준** |
| target | O | X | HMS에 target 추가 검토 |
| start_date / end_date | O | X | HMS에 추가 검토 |
| is_active | X | O | HMS 기준 유지 |
| created_at / updated_at | X | O | HMS 기준 유지 |

**결정**: HMS `HospitalRule` 기준 사용. spring_llm의 `target`, `start_date`, `end_date`는 추가 여부 확인 후 결정.

---

### 4.3 Reservation 통합

| 항목 | spring_llm (reservation_tb) | HMS (reservation) | 전략 |
|------|-----------------------------|-------------------|------|
| doctor_id | O (FK) | 확인 필요 | HMS 기준 |
| staff_id | O (FK, nullable) | 확인 필요 | HMS 기준 |
| start_time / end_time | O | 확인 필요 | 없으면 HMS에 추가 |
| status | String | 확인 필요 | HMS 기준 |

**결정**: HMS `Reservation` 기준. `start_time`, `end_time` 없으면 추가.

---

### 4.4 Doctor / Staff / DoctorSchedule

| Entity | 전략 |
|--------|------|
| `Doctor` | **HMS 것 사용** — Department 연관 등 HMS 스키마 준수 |
| `Staff` | **HMS 것 사용** — StaffRole enum 준수 |
| `DoctorSchedule` | HMS에 없으면 신규 추가, 있으면 HMS 기준 |

---

## Step 5: LLM 전용 신규 Entity 확인

HMS domain에 없는 경우 Task 4에서 추가 예정:

| Entity | 테이블 | 비고 |
|--------|--------|------|
| `MedicalHistory` | medical_history | 의료 상담 이력 |
| `MedicalContent` | medical_content | 의학 콘텐츠 |
| `MedicalQa` | medical_qa | 의학 Q&A |
| `MedicalDomain` | medical_domain | 진료 도메인 |

---

## Step 6: HMS llm 패키지 현황 확인

확인 대상: `com.smartclinic.hms.llm/` 하위 구조

- 기존 `LlmService`, `LlmController` 파악
- Claude API 연동 방식 확인 (RestClient)
- spring_llm의 Python WebClient 방식과 공존 전략 결정

---

## 완료 기준

- [ ] HMS 패키지 구조 전체 파악
- [ ] 6개 중복 Entity 비교 및 통합 전략 확정
- [ ] LLM 전용 신규 Entity 목록 확정
- [ ] HMS llm 패키지 현황 파악
- [ ] Task 2 (build.gradle 의존성 + 설정 파일 병합) 진행 준비 완료

---

## 다음 작업

완료 후 → `W4-2-workflow.md` (Task 2: build.gradle 의존성 및 설정 파일 병합)
