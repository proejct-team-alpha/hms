# W4-4 리포트 - Entity 및 Repository 이식

## 작업 개요
- **작업명**: spring-python-llm-exam-mng Entity/Repository를 HMS domain 패키지로 이식
- **수정 파일**:
  - `src/main/java/com/smartclinic/hms/domain/HospitalRule.java`
  - `src/main/java/com/smartclinic/hms/domain/Reservation.java`
- **신규 Entity**:
  - `domain/DoctorSchedule.java`
  - `domain/MedicalHistory.java`
  - `domain/MedicalContent.java`
  - `domain/MedicalQa.java`
  - `domain/MedicalDomain.java`
- **신규 Repository**:
  - `domain/ChatbotHistoryRepository.java`
  - `domain/DoctorScheduleRepository.java`
  - `domain/MedicalHistoryRepository.java`
  - `domain/MedicalContentRepository.java`
  - `domain/MedicalQaRepository.java`
  - `domain/MedicalDomainRepository.java`

## 작업 내용

### 스키마 비교 결과

#### HospitalRule vs MedicalRule — 통합 전략: HospitalRule 확장
| 필드 | HospitalRule | MedicalRule | 처리 |
|---|---|---|---|
| id | Long (IDENTITY) | Integer | HMS 유지 |
| title | String(200) | String(200) | HMS 유지 |
| content | TEXT | LONGTEXT | HMS 유지 |
| category | HospitalRuleCategory(enum) | String(50) | HMS enum 유지 |
| active | boolean | 없음 | HMS 유지 |
| createdAt / updatedAt | 있음 | createdAt만 | HMS 유지 |
| **target** | 없음 → **추가** | String(100) | 확장 |
| **startDate** | 없음 → **추가** | LocalDate | 확장 |
| **endDate** | 없음 → **추가** | LocalDate | 확장 |

#### Reservation — startTime/endTime 확장
| 필드 | HMS | spring-llm | 처리 |
|---|---|---|---|
| staff | 없음 | @ManyToOne Staff | 불필요 (HMS 구조상 patient/department 보유) |
| **startTime** | 없음 → **추가** | LocalTime | 확장 |
| **endTime** | 없음 → **추가** | LocalTime | 확장 |

### 1. HospitalRule 필드 확장
`target`, `startDate`, `endDate` 3개 필드 추가. 기존 `create()`, `update()` 팩토리 메서드는 수정 없음 — 선택적 필드이므로 setter로 후처리 가능.

### 2. Reservation 필드 확장
`startTime`, `endTime` 추가. spring-llm의 `staff` FK는 HMS에서 `patient`/`department`로 대체되므로 이식 제외.

### 3. DoctorSchedule (신규)
- HMS `Doctor` 단방향 `@ManyToOne` 참조
- `Doctor`에 `@OneToMany` 추가 없음

### 4. MedicalHistory (신규)
- HMS `Staff` 단방향 `@ManyToOne` 참조
- `getStaffId()` 편의 메서드 유지

### 5~7. MedicalContent, MedicalQa, MedicalDomain (신규)
- FK 없음, 패키지명만 `com.smartclinic.hms.domain`으로 변경하여 이식

### 8. ChatbotHistoryRepository (신규)
HMS에 `ChatbotHistory` Entity는 있었으나 Repository가 없었음. 신규 생성.
- `findByStaffIdOrderByCreatedAtDesc` — 직원별 이력 조회
- `findBySessionIdOrderByCreatedAtAsc` — 세션별 대화 조회

### 9. LLM Repository 5개 (신규)
- `DoctorScheduleRepository` — `findByDoctorId`, `findByDoctorIdAndDayOfWeek`
- `MedicalHistoryRepository` — `findBySessionId`, `findByStaffIdOrderByCreatedAtDesc`
- `MedicalContentRepository`, `MedicalQaRepository`, `MedicalDomainRepository` — 기본 JpaRepository

## 빌드 결과
```
BUILD SUCCESSFUL in 3s
```

## 특이사항
- `ChatHistory`(spring-llm) → HMS `ChatbotHistory` 전환: Repository 신규 생성으로 대체, 별도 Entity 이식 없음
- `MedicalRule` Entity는 HMS에 이식하지 않음 — `HospitalRule` 확장으로 대체
- `MedicalDomainRepository`의 ID 타입은 `Integer` (`domainId` 필드가 `@Id`이며 Integer 타입)
- `HospitalRule.update()` 메서드는 추가 필드(`target`, `startDate`, `endDate`) 미반영 — Task 6 Controller/DTO 이식 시 필요하면 수정
