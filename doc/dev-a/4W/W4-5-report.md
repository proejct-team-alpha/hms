# W4-5 리포트 - Service 이식

## 작업 개요
- **작업명**: spring-python-llm-exam-mng Service 레이어를 HMS `llm/` 패키지로 이식
- **수정 파일**:
  - `doctor/DoctorRepository.java` — `findByDepartmentNameAndActive` 추가
  - `reservation/reservation/ReservationRepository.java` — `countByDoctor_IdAndReservationDateAndStartTime` 추가
  - `domain/DoctorScheduleRepository.java` — `findByDoctor_IdAndIsAvailableTrue` 추가
- **신규 DTO** (`llm/dto/`):
  - `LlmRequest.java`, `LlmResponse.java`
  - `DoctorDto.java`, `DoctorScheduleDto.java`, `DoctorWithScheduleDto.java`
  - `LlmReservationRequest.java`, `LlmReservationResponse.java`
- **신규 Service** (`llm/service/`):
  - `LlmResponseParser.java`, `ChatService.java`, `MedicalService.java`
  - `DoctorService.java`, `LlmReservationService.java`

## 작업 내용

### 1. Repository 메서드 추가

#### DoctorRepository — 진료과명 + 활성 의사 조회
HMS `Doctor`는 `department`가 Entity FK(`@ManyToOne`)이므로 derived query 불가 → `@Query` JPQL 사용.
```java
@Query("SELECT d FROM Doctor d JOIN FETCH d.staff JOIN FETCH d.department " +
       "WHERE d.department.name = :deptName AND d.staff.active = true")
List<Doctor> findByDepartmentNameAndActive(@Param("deptName") String deptName);
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 특정 진료과에 소속된 활성 의사 목록을 데이터베이스에서 조회하는 JPQL 쿼리입니다. `JOIN FETCH`로 의사(`Doctor`), 직원 정보(`Staff`), 진료과(`Department`) 데이터를 한 번의 쿼리로 모두 가져옵니다.
> - **왜 이렇게 썼는지**: `Doctor`의 `department` 필드가 문자열이 아니라 `Department` Entity와의 관계(외래키)로 연결되어 있기 때문에, Spring Data JPA의 자동 쿼리 생성 방식으로는 표현하기 어렵습니다. `@Query`로 직접 JPQL을 작성하면 복잡한 조건도 명확하게 표현할 수 있습니다.
> - **쉽게 말하면**: "내과에서 현재 근무 중인 의사 전원을 한 번에 가져와"라는 조건을 Java 코드로 데이터베이스에 전달하는 방법입니다.

#### ReservationRepository — 슬롯 중복 체크
Task 4에서 추가된 `startTime` 필드를 활용한 derived query.
```java
long countByDoctor_IdAndReservationDateAndStartTime(Long doctorId, LocalDate date, LocalTime startTime);
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 특정 의사, 특정 날짜, 특정 시작 시간에 이미 예약이 몇 건 있는지 숫자를 반환합니다. 이 숫자가 0보다 크면 해당 시간대는 이미 예약된 것입니다.
> - **왜 이렇게 썼는지**: Spring Data JPA는 `countBy조건1And조건2And조건3` 형식의 메서드 이름을 `SELECT COUNT(*) WHERE 조건1=? AND 조건2=? AND 조건3=?` SQL로 자동 변환합니다. 가용 슬롯을 계산할 때 중복 예약 여부를 확인하기 위해 사용합니다.
> - **쉽게 말하면**: "이 의사의 이 날 이 시간에 예약이 이미 있나요?"를 확인하는 카운터입니다.

#### DoctorScheduleRepository — 활성 스케줄 조회
```java
List<DoctorSchedule> findByDoctor_IdAndIsAvailableTrue(Long doctorId);
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 특정 의사의 예약 가능(`isAvailable = true`)한 스케줄 목록을 조회합니다.
> - **왜 이렇게 썼는지**: `findByDoctor_IdAndIsAvailableTrue`에서 `Doctor_Id`는 연관된 `Doctor` Entity의 `id` 필드를 가리킵니다(`_`로 연관 관계 탐색). `AndIsAvailableTrue`는 별도 조건값 없이 `isAvailable = true`를 자동으로 의미합니다.
> - **쉽게 말하면**: "이 의사의 진료 가능한 시간표 목록을 가져와"라는 조회입니다.

### 2. LLM DTO 생성

#### DoctorDto / DoctorWithScheduleDto — HMS Doctor 구조 어댑터
spring-llm의 `Doctor` Entity는 `name`, `department`(String), `isActive` 필드를 직접 보유.
HMS `Doctor`는 이 값들이 관계를 통해 존재하므로 `from()` 팩토리 메서드를 어댑터 패턴으로 수정했다.

| spring-llm Doctor | HMS 대응 |
|---|---|
| `doctor.getName()` | `doctor.getStaff().getName()` |
| `doctor.getDepartment()` (String) | `doctor.getDepartment().getName()` |
| `doctor.isActive()` | `doctor.getStaff().isActive()` |
| `hospital`, `bio` 필드 | HMS 미지원 → 제외 |

#### LlmReservationResponse
- spring-llm `ReservationResponse.Max.from()`의 `getId()` 반환 타입이 `Integer` → HMS `Reservation.id`는 `Long`으로 수정
- `getStatus()` → HMS `ReservationStatus` enum → `.name()` 으로 String 변환

### 3. LlmResponseParser
패키지명만 변경. 진료과 추출 정규식 로직 변경 없음.

### 4. ChatService 이식

| 변경 항목 | spring-llm | HMS |
|---|---|---|
| History Entity | `ChatHistory` | `ChatbotHistory` |
| Repository | `ChatHistoryRepository` | `ChatbotHistoryRepository` |
| History 생성 | `new ChatHistory(staff, sessionId, q, a)` | `ChatbotHistory.create(sessionId, staff, q, a)` |
| StaffRepository 패키지 | `repository/` | `auth/` |

### 5. MedicalService 이식
- `StaffRepository` → `auth/StaffRepository`
- Jackson 3.x(`tools.jackson`) — Spring Boot 4.0.3 기반으로 원본 그대로 유지
- staffId는 서비스 파라미터로 전달받음 (Controller에서 Security principal 추출 예정, Task 6)

### 6. DoctorService 이식
- `doctorRepository.findByDepartmentAndIsActiveTrue()` → `findByDepartmentNameAndActive()`
- `doctorScheduleRepository.findByDoctorIdAndIsAvailableTrue()` → `findByDoctor_IdAndIsAvailableTrue()`
- DTO `from()` 메서드가 HMS Doctor 구조를 처리하도록 DoctorDto/DoctorWithScheduleDto 어댑터 적용

### 7. LlmReservationService (신규)
spring-llm `ReservationService`의 예약 생성 로직(`createReservation`)은 제외하고
**가용 슬롯 조회(`getAvailableSlots`)만 추출**하여 별도 서비스로 분리했다.
- 예약 생성은 기존 HMS `ReservationService`가 담당
- 슬롯 조회 시 Doctor 이름: `doctor.getStaff().getName()`

## 빌드 결과
```
BUILD SUCCESSFUL in 3s
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 여러 Service 클래스와 DTO를 추가한 후 프로젝트 전체가 컴파일 오류 없이 빌드되었음을 나타냅니다.
> - **왜 이렇게 썼는지**: Service 간 의존 관계가 복잡할수록 순환 참조(A가 B를 참조하고 B가 A를 참조)나 Bean 생성 오류가 발생할 수 있습니다. 빌드 및 서버 기동 성공이 이를 검증합니다.
> - **쉽게 말하면**: 여러 직원이 새로 합류한 후 팀 전체가 문제없이 출근 완료한 상태입니다.

## 특이사항
- `MedicalService`의 `callLlmApi()` 메서드는 `callMedicalLlmApi()`와 중복 → 이식 시 `callMedicalLlmApi()`로 통합
- `LlmReservationRequest.Save`는 현재 미사용 — Task 6 Controller 이식 시 활용 예정
- `X-Staff-Id` 헤더 → Security principal 전환은 Service 파라미터 방식 유지, Controller(Task 6)에서 처리
