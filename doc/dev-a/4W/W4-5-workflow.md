# W4-5 Service 이식

## 작업 목표
spring-python-llm-exam-mng의 Service 레이어를 HMS `llm/` 패키지로 이식한다.
- HMS 기존 LlmService(Claude API): 실질적 미구현 상태 — 삭제 없이 그대로 유지
- `ChatService`, `MedicalService`, `DoctorService`, `LlmResponseParser` 이식
- `LlmReservationService` 신규 작성 (ReservationService 분리)
- `X-Staff-Id` 헤더 → Spring Security principal 전환

## 작업 목록
<!-- TODO 1. Repository 메서드 추가 (DoctorRepository, ReservationRepository, DoctorScheduleRepository) -->
<!-- TODO 2. LLM 전용 DTO 생성 (llm/dto/) — 컴파일 선행 조건 -->
<!-- TODO 3. LlmResponseParser 이식 -->
<!-- TODO 4. ChatService 이식 (ChatbotHistory 전환, Security principal) -->
<!-- TODO 5. MedicalService 이식 (Security principal) -->
<!-- TODO 6. DoctorService 이식 (HMS Doctor 구조 어댑터) -->
<!-- TODO 7. LlmReservationService 신규 작성 -->
<!-- TODO 8. 빌드 확인 -->

## 진행 현황
- [x] 1. Repository 메서드 추가
- [x] 2. LLM DTO 생성
- [x] 3. LlmResponseParser 이식
- [x] 4. ChatService 이식
- [x] 5. MedicalService 이식
- [x] 6. DoctorService 이식
- [x] 7. LlmReservationService 신규
- [x] 8. 빌드 확인 — BUILD SUCCESSFUL

## 수정/추가 파일
**수정 (Repository 메서드 추가)**
- `doctor/DoctorRepository.java` — `findByDepartment_NameAndStaff_ActiveTrue` 추가
- `reservation/reservation/ReservationRepository.java` — `countByDoctorIdAndReservationDateAndStartTime` 추가
- `domain/DoctorScheduleRepository.java` — `findByDoctorIdAndIsAvailableTrue` 추가

**신규 DTO**
- `llm/dto/LlmRequest.java`
- `llm/dto/LlmResponse.java`
- `llm/dto/DoctorDto.java`
- `llm/dto/DoctorScheduleDto.java`
- `llm/dto/DoctorWithScheduleDto.java`
- `llm/dto/LlmReservationRequest.java`
- `llm/dto/LlmReservationResponse.java`

**신규 Service**
- `llm/service/LlmResponseParser.java`
- `llm/service/ChatService.java`
- `llm/service/MedicalService.java`
- `llm/service/DoctorService.java`
- `llm/service/LlmReservationService.java`

---

## 상세 내용

### 핵심 차이점 분석

#### HMS Doctor vs spring-llm Doctor
| spring-llm | HMS 대응 |
|---|---|
| `doctor.getName()` | `doctor.getStaff().getName()` |
| `doctor.getDepartment()` (String) | `doctor.getDepartment().getName()` |
| `doctor.isActive()` | `doctor.getStaff().isActive()` |
| `findByDepartmentAndIsActiveTrue(String)` | `findByDepartment_NameAndStaff_ActiveTrue(String)` |

#### X-Staff-Id → Security principal 전환
```java
// 기존 (헤더 방식)
Long staffId = Long.parseLong(request.getHeader("X-Staff-Id"));

// 변환 (Security principal)
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String username = auth.getName();
Staff staff = staffRepository.findByUsernameAndActiveTrue(username).orElseThrow(...);
Long staffId = staff.getId();
```

---

### 1. Repository 메서드 추가

#### DoctorRepository — 진료과명 + 활성 의사 조회
```java
@Query("SELECT d FROM Doctor d JOIN FETCH d.staff JOIN FETCH d.department " +
       "WHERE d.department.name = :deptName AND d.staff.active = true")
List<Doctor> findByDepartment_NameAndStaff_ActiveTrue(@Param("deptName") String deptName);
```

#### ReservationRepository — 슬롯 중복 체크
```java
long countByDoctorIdAndReservationDateAndStartTime(Long doctorId, LocalDate date, LocalTime startTime);
```

#### DoctorScheduleRepository — 활성 스케줄 조회
```java
List<DoctorSchedule> findByDoctorIdAndIsAvailableTrue(Long doctorId);
```

---

### 2. LLM DTO (컴파일 선행 생성)

#### LlmRequest / LlmResponse
- spring-llm 원본 그대로 패키지명만 변경

#### DoctorDto
```java
// HMS Doctor 구조(staff/department 관계) 기반으로 from() 어댑터 수정
public static DoctorDto from(Doctor doctor) {
    return new DoctorDto(
        doctor.getId(),
        doctor.getStaff().getName(),       // doctor.getName() → staff.getName()
        doctor.getDepartment().getName(),   // String → Department.getName()
        doctor.getSpecialty(),
        doctor.getStaff().isActive()       // isActive() → staff.isActive()
    );
}
```

#### DoctorScheduleDto
- spring-llm 원본 그대로, HMS DoctorSchedule 참조

#### LlmReservationRequest / LlmReservationResponse
- spring-llm `ReservationRequest` / `ReservationResponse` 패키지명 변경
- `ReservationResponse.Max.from()` — id를 `Long`으로 수정 (HMS Reservation id는 Long)

---

### 3. LlmResponseParser
- 패키지명만 변경, 코드 변경 없음

---

### 4. ChatService 이식
| 변경 항목 | spring-llm | HMS |
|---|---|---|
| History Entity | `ChatHistory` | `ChatbotHistory` |
| Repository | `ChatHistoryRepository` | `ChatbotHistoryRepository` |
| Repository 위치 | `repository/` | `domain/` |
| History 생성 | `new ChatHistory(staff, sessionId, q, a)` | `ChatbotHistory.create(sessionId, staff, q, a)` |
| Staff 조회 | `staffRepository.findById(staffId)` | `staffRepository.findByUsernameAndActiveTrue(username)` |
| 패키지 | `com.sample.llm.service` | `com.smartclinic.hms.llm.service` |

---

### 5. MedicalService 이식
| 변경 항목 | spring-llm | HMS |
|---|---|---|
| StaffRepository | `com.sample.llm.repository` | `com.smartclinic.hms.auth` |
| MedicalHistoryRepository | `repository/` | `domain/` |
| staffId 획득 | 파라미터로 직접 전달 | Security principal에서 추출 후 전달 |
| 패키지 | `com.sample.llm.service` | `com.smartclinic.hms.llm.service` |

---

### 6. DoctorService 이식
| 변경 항목 | spring-llm | HMS |
|---|---|---|
| DoctorRepository | `findByDepartmentAndIsActiveTrue` | `findByDepartment_NameAndStaff_ActiveTrue` |
| DoctorScheduleRepository | `repository/` | `domain/` |
| DoctorDto.from() | spring-llm Doctor 기반 | HMS Doctor 기반 (staff/department 관계) |
| 패키지 | `com.sample.llm.service` | `com.smartclinic.hms.llm.service` |

---

### 7. LlmReservationService (신규)
- spring-llm `ReservationService`를 기반으로 HMS 구조에 맞게 작성
- `ReservationRepository` → HMS `reservation/reservation/ReservationRepository`
- `DoctorRepository` → HMS `doctor/DoctorRepository`
- Doctor 이름: `doctor.getStaff().getName()`
- Doctor ID 타입: `Long`

## 수용 기준
- [ ] `./gradlew build` 오류 없음
- [ ] Service Bean 정상 생성 (순환 참조 없음)
