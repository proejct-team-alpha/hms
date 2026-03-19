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

#### ReservationRepository — 슬롯 중복 체크
Task 4에서 추가된 `startTime` 필드를 활용한 derived query.
```java
long countByDoctor_IdAndReservationDateAndStartTime(Long doctorId, LocalDate date, LocalTime startTime);
```

#### DoctorScheduleRepository — 활성 스케줄 조회
```java
List<DoctorSchedule> findByDoctor_IdAndIsAvailableTrue(Long doctorId);
```

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

## 특이사항
- `MedicalService`의 `callLlmApi()` 메서드는 `callMedicalLlmApi()`와 중복 → 이식 시 `callMedicalLlmApi()`로 통합
- `LlmReservationRequest.Save`는 현재 미사용 — Task 6 Controller 이식 시 활용 예정
- `X-Staff-Id` 헤더 → Security principal 전환은 Service 파라미터 방식 유지, Controller(Task 6)에서 처리
