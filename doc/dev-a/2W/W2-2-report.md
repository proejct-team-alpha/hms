# W2-2. 진료과 → 전문의 동적 조회 (AJAX) — 작업 리포트

**작업일**: 2026-03-09

---

## 작업 내용

진료과 select 변경 시 AJAX로 해당 과 소속 전문의 목록을 DB에서 조회하여
doctor select를 동적으로 채우도록 구현했다.
구현 과정에서 발생한 버그 3건을 추가로 수정했다.

---

## 변경/생성 파일

| 파일 | 구분 | 작업 내용 |
|------|------|----------|
| `sql_test.sql` | 수정 | 진료과 4개, staff 9명, 의사 5명으로 샘플 데이터 확장 |
| `doctor/DoctorRepository.java` | 임시 생성 (B 영역) | `@Query` + `JOIN FETCH` 로 doctor + staff 한 번에 조회 |
| `doctor/DoctorDto.java` | 임시 생성 (B 영역) | 응답 DTO (id, name) |
| `reservation/ReservationService.java` | 수정 | `@Transactional(readOnly = true)` 추가, `getDoctorsByDepartment()` 구현 |
| `reservation/ReservationApiController.java` | 신규 생성 | `GET /api/reservation/doctors` 엔드포인트 |
| `direct-reservation.mustache` | 수정 | department value id 교체, doctor 옵션 동적화, AJAX JS 추가 |

---

## 상세 변경 사항

### 1. sql_test.sql

- department: 2개 → 4개 (소아과, 이비인후과 추가)
- staff: 5명 → 9명 (doctor02~05 추가)
- doctor: 1명 → 5명 (외과 2명, 소아과 1명, 이비인후과 1명 추가)

### 2. doctor/DoctorRepository.java (임시 생성)

```java
// B 작업자 정식 구현 전까지 임시 생성
// JOIN FETCH로 staff 즉시 로딩 → LazyInitializationException 방지
@Query("SELECT d FROM Doctor d JOIN FETCH d.staff WHERE d.department.id = :departmentId")
List<Doctor> findByDepartment_Id(@Param("departmentId") Long departmentId);
```

### 3. doctor/DoctorDto.java (임시 생성)

```java
public DoctorDto(Doctor doctor) {
    this.id = doctor.getId();
    this.name = doctor.getStaff().getName();
}
```

### 4. ReservationService.java

```java
@Transactional(readOnly = true)  // Hibernate 세션 유지 (lazy 로딩 보호)
public List<DoctorDto> getDoctorsByDepartment(Long departmentId) {
    return doctorRepository.findByDepartment_Id(departmentId)
            .stream().map(DoctorDto::new).toList();
}
```

### 5. ReservationApiController.java

```java
// @RequestParam에 이름 명시 필수 (컴파일러 -parameters 플래그 미설정)
@GetMapping("/doctors")
public List<DoctorDto> getDoctors(@RequestParam("departmentId") Long departmentId) {
    return reservationService.getDoctorsByDepartment(departmentId);
}
```

### 6. direct-reservation.mustache

| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| department option value | `"내과"`, `"외과"` ... | `"1"`, `"2"` ... (DB id) |
| doctor select | 하드코딩 옵션 3개 | 초기 "선택해주세요"만, AJAX로 동적 교체 |
| JS | AI 추천 로직만 | department change 이벤트 + fetch AJAX 추가 |

---

## 버그 수정 이력

| 순서 | 파일 | 버그 | 원인 | 수정 |
|------|------|------|------|------|
| 1 | `ReservationApiController.java` | 의사 목록 미조회 | `@RequestParam(value = "doctorId")`로 파라미터 이름 오기입 | `@RequestParam("departmentId")`로 수정 |
| 2 | `DoctorRepository.java` | `LazyInitializationException` | `staff`가 LAZY 로딩인데 트랜잭션 밖에서 접근 | `JOIN FETCH d.staff` 쿼리로 즉시 로딩 |
| 3 | `ReservationApiController.java` | `IllegalArgumentException` | `@RequestParam` 이름 미명시 + 컴파일러 `-parameters` 플래그 없음 | `@RequestParam("departmentId")` 명시 |
| 4 | `DoctorRepository.java` | 트랜잭션 충돌 위험 | `jakarta.transaction.@Transactional`을 interface에 잘못 추가 | 제거 후 Spring `@Query` 방식으로 교체 |

---

## SecurityConfig 협업 내용

`/api/reservation/**` 는 비회원 접근이 필요하므로 config 담당자가 `permitAll()` 설정 완료.

```java
.requestMatchers("/", "/reservation/**", "/api/reservation/**").permitAll()
```

---

## 수용 기준 확인

- [x] 진료과 선택 시 해당 과 전문의만 doctor select에 표시
- [x] 진료과 변경 시 doctor select 초기화 후 재조회
- [x] `GET /api/reservation/doctors?departmentId=1` → JSON 배열 응답
- [x] sql_test.sql에 진료과 4개 + 의사 5명 샘플 데이터 존재
- [x] 비로그인 상태에서 `/api/reservation/doctors` 호출 시 정상 응답

---

## 참조 문서

- `doc/dev-a/W2-2-workflow.md`
