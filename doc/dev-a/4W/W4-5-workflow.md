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

### DoctorRepository — 추가 쿼리

```java
@Query("SELECT d FROM Doctor d JOIN FETCH d.staff JOIN FETCH d.department " +
       "WHERE d.department.name = :deptName AND d.staff.active = true")
List<Doctor> findByDepartment_NameAndStaff_ActiveTrue(@Param("deptName") String deptName);
```

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
