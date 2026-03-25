# W4-5 Workflow — Service 이식

> **작성일**: 4W
> **브랜치**: `feature/Llm`
> **목표**: spring-python-llm Service 레이어를 HMS `llm/` 패키지로 이식

---

## 전체 흐름

```
Repository 메서드 추가 → LLM DTO 생성
  → ChatService, MedicalService, DoctorService, LlmResponseParser 이식
  → LlmReservationService 신규 작성
  → X-Staff-Id 헤더 → Security principal 전환
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 기존 LlmService | 실질적 미구현 상태 → 삭제 없이 유지 |
| 인증 방식 | X-Staff-Id 헤더 → Spring Security principal 전환 |
| HMS Doctor 구조 | `doctor.getName()` → `doctor.getStaff().getName()` |
| LlmReservationService | ReservationService에서 분리, 슬롯 조회 전담 |

---

## 실행 흐름

```
[1] Repository 메서드 추가 (DoctorRepository, ReservationRepository, DoctorScheduleRepository)
[2] LLM DTO 신규 생성 (llm/dto/)
[3] LlmResponseParser 이식 (패키지명만 변경)
[4] ChatService 이식 (ChatHistory → ChatbotHistory, Security principal 적용)
[5] MedicalService 이식 (auth/StaffRepository 참조)
[6] DoctorService 이식 (HMS Doctor 어댑터)
[7] LlmReservationService 신규 작성
[8] ./gradlew build 검증
```

---

## UI Mockup

```
[Service 이식 작업 — UI 없음]
```

---

## 작업 목록

1. `DoctorRepository` — `findByDepartment_NameAndStaff_ActiveTrue(@Query JPQL)` 추가
2. `ReservationRepository` — `countByDoctor_IdAndReservationDateAndStartTime` 추가
3. `DoctorScheduleRepository` — `findByDoctor_IdAndIsAvailableTrue` 추가
4. LLM DTO 신규 생성 (`LlmRequest`, `LlmResponse`, `DoctorDto`, `DoctorScheduleDto`, `DoctorWithScheduleDto`, `LlmReservationRequest`, `LlmReservationResponse`)
5. `LlmResponseParser.java` 이식 (패키지명만 변경)
6. `ChatService.java` 이식 (`ChatbotHistory` 전환, Security principal)
7. `MedicalService.java` 이식 (auth/StaffRepository 참조)
8. `DoctorService.java` 이식 (HMS Doctor 구조 어댑터)
9. `LlmReservationService.java` 신규 작성
10. `./gradlew build` 검증

---

## 작업 진행내용

- [x] Repository 메서드 추가
- [x] LLM DTO 생성
- [x] LlmResponseParser 이식
- [x] ChatService 이식
- [x] MedicalService 이식
- [x] DoctorService 이식
- [x] LlmReservationService 신규
- [x] 빌드 확인 — BUILD SUCCESSFUL

---

## 실행 흐름에 대한 코드

### X-Staff-Id → Security principal 전환

```java
// 기존 (헤더 방식)
Long staffId = Long.parseLong(request.getHeader("X-Staff-Id"));

// 변환 (Security principal)
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
Staff staff = staffRepository.findByUsernameAndActiveTrue(auth.getName()).orElseThrow();
Long staffId = staff.getId();
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 로그인한 직원이 누구인지 확인하는 방법을 변경합니다. 기존에는 HTTP 요청 헤더에 직접 직원 ID를 담아 보냈지만, 변환 후에는 Spring Security가 관리하는 로그인 세션 정보에서 현재 사용자 이름을 읽어 직원 ID를 조회합니다.
> - **왜 이렇게 썼는지**: 헤더에 `X-Staff-Id`를 직접 담는 방식은 누구든 헤더를 조작해 다른 직원인 척 할 수 있어 보안에 취약합니다. `SecurityContextHolder`는 Spring이 로그인 세션에서 안전하게 관리하는 인증 정보 저장소이므로, 이를 사용하면 위조가 불가능합니다.
> - **쉽게 말하면**: "내 이름표는 1번이야"라고 직접 말하는 것(헤더 방식)에서, 회사 출입증 시스템이 자동으로 확인하는 방식(Security 방식)으로 바꾼 것입니다.

### DoctorDto — HMS Doctor 어댑터

```java
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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: `Doctor` Entity를 `DoctorDto`(데이터 전달 객체)로 변환하는 팩토리 메서드입니다. HMS의 `Doctor`는 이름, 활성 여부를 직접 갖지 않고 `Staff`(직원) 엔티티를 통해 접근하므로, 이를 변환 시 처리합니다.
> - **왜 이렇게 썼는지**: 원본(spring-llm) `Doctor`는 `getName()`, `isActive()`를 직접 가지고 있었지만, HMS `Doctor`는 `Staff`와 1:1 관계로 설계되어 있습니다. 어댑터 패턴(구조가 다른 두 시스템을 연결하는 변환기)을 적용해 이 차이를 `from()` 메서드 안에서 투명하게 처리합니다.
> - **쉽게 말하면**: 서로 다른 규격의 플러그를 연결하는 어댑터처럼, HMS 의사 데이터 구조와 LLM이 기대하는 데이터 구조 사이의 차이를 이 메서드가 맞춰줍니다.

### DoctorRepository — 추가 쿼리

```java
@Query("SELECT d FROM Doctor d JOIN FETCH d.staff JOIN FETCH d.department " +
       "WHERE d.department.name = :deptName AND d.staff.active = true")
List<Doctor> findByDepartment_NameAndStaff_ActiveTrue(@Param("deptName") String deptName);
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 특정 진료과에 소속된 활성 의사 목록을 조회하는 JPQL 쿼리입니다. `JOIN FETCH`를 사용해 `Doctor`, `Staff`, `Department` 데이터를 한 번의 쿼리로 모두 가져옵니다.
> - **왜 이렇게 썼는지**: HMS `Doctor`는 `department`가 문자열이 아닌 `Department` Entity(외래키 관계)이므로, Spring Data JPA의 메서드 이름 자동 생성이 복잡해집니다. 따라서 `@Query`로 직접 JPQL을 작성했습니다. `JOIN FETCH`는 연관된 데이터를 미리 함께 불러와 N+1 쿼리 문제(데이터를 하나씩 따로 조회하는 비효율)를 방지합니다.
> - **쉽게 말하면**: "내과 소속이고 현재 근무 중인 의사 전원을 한 번에 가져와"라는 SQL을 Java 코드로 표현한 것입니다.

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| 빌드 | ./gradlew build | BUILD SUCCESSFUL |
| Service Bean | 서버 기동 | 순환 참조 없음 |

---

## 완료 기준

- [x] `./gradlew build` 오류 없음
- [x] Service Bean 정상 생성 (순환 참조 없음)
