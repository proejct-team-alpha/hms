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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: `@Query`로 직접 JPQL 쿼리를 작성합니다. `JOIN FETCH d.staff`는 Doctor를 조회할 때 연결된 Staff 정보도 함께(즉시) 가져오도록 합니다.
> - **왜 이렇게 썼는지**: JPA는 기본적으로 연관 데이터(staff)를 나중에 필요할 때 불러오는 지연 로딩(Lazy Loading)을 사용합니다. 트랜잭션이 끝난 후 staff에 접근하면 `LazyInitializationException`이 발생합니다. `JOIN FETCH`를 사용하면 처음부터 함께 가져오므로 이 오류를 방지합니다.
> - **쉽게 말하면**: 의사 정보를 가져올 때 소속 직원 정보도 한 번에 같이 가져오라고 DB에 요청하는 것입니다. 나중에 다시 가져오려면 창고(DB)가 이미 닫혀서 문제가 생기기 때문입니다.

### 3. doctor/DoctorDto.java (임시 생성)

```java
public DoctorDto(Doctor doctor) {
    this.id = doctor.getId();
    this.name = doctor.getStaff().getName();
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: `Doctor` 엔티티 객체를 받아서 id와 이름만 추출하여 `DoctorDto` 객체를 생성하는 생성자입니다.
> - **왜 이렇게 썼는지**: `doctor.getStaff().getName()`은 Doctor 엔티티가 직접 이름을 갖지 않고, Staff 엔티티를 통해 이름을 가져오는 구조이기 때문입니다. JOIN FETCH로 staff를 함께 로드했기 때문에 이 접근이 안전합니다.
> - **쉽게 말하면**: 의사 파일에서 ID와 이름표(직원 파일에 있음)만 복사해 간결한 카드를 만드는 것입니다.

### 4. ReservationService.java

```java
@Transactional(readOnly = true)  // Hibernate 세션 유지 (lazy 로딩 보호)
public List<DoctorDto> getDoctorsByDepartment(Long departmentId) {
    return doctorRepository.findByDepartment_Id(departmentId)
            .stream().map(DoctorDto::new).toList();
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 진료과 ID로 해당 과 의사 목록을 DB에서 조회하여 DTO 리스트로 변환해 반환합니다.
> - **왜 이렇게 썼는지**: `@Transactional(readOnly = true)`는 읽기 전용 트랜잭션을 열어 Hibernate 세션을 유지합니다. 이렇게 하면 메서드 실행 중에 lazy 로딩이 필요한 경우에도 세션이 열려 있어 안전합니다. 읽기만 하므로 DB 성능도 최적화됩니다.
> - **쉽게 말하면**: "창고 문을 열어둔 채로" 의사 목록을 꺼내고 정리하는 작업입니다. 문이 열려 있어야 필요한 추가 정보(staff)도 꺼낼 수 있습니다.

### 5. ReservationApiController.java

```java
// @RequestParam에 이름 명시 필수 (컴파일러 -parameters 플래그 미설정)
@GetMapping("/doctors")
public List<DoctorDto> getDoctors(@RequestParam("departmentId") Long departmentId) {
    return reservationService.getDoctorsByDepartment(departmentId);
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: `/api/reservation/doctors?departmentId=1`로 GET 요청이 오면 해당 진료과 의사 목록을 JSON으로 반환하는 API 엔드포인트입니다.
> - **왜 이렇게 썼는지**: `@RequestParam("departmentId")`처럼 이름을 명시적으로 지정한 이유는, 이 프로젝트의 컴파일러에 `-parameters` 플래그가 설정되지 않아 파라미터 이름을 자동으로 인식하지 못하기 때문입니다. 이름을 명시하지 않으면 오류가 발생합니다.
> - **쉽게 말하면**: URL에서 "departmentId"라는 이름의 값을 꺼내 쓰는 API 창구입니다. 이름표를 명확히 붙여두어야 Spring이 찾을 수 있습니다.

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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: Spring Security 설정에서 `/`, `/reservation/**`, `/api/reservation/**` 경로는 로그인 없이도 누구나 접근할 수 있도록 허용합니다.
> - **왜 이렇게 썼는지**: 예약 관련 페이지와 API는 비회원도 사용해야 하므로 인증 없이 접근을 허용해야 합니다. `/**`는 해당 경로 하위의 모든 URL을 의미합니다.
> - **쉽게 말하면**: 병원 예약 창구는 회원 카드 없이도 누구나 이용할 수 있도록 출입 제한을 풀어두는 것입니다.

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
